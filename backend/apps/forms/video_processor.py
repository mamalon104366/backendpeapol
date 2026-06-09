import os
import json
import tempfile
import shutil
from pathlib import Path
from functools import lru_cache

from django.conf import settings
import imageio_ffmpeg as ffmpeg
from google import genai
from pydantic import BaseModel
from typing import List, Optional

def _ensure_ffmpeg_alias() -> str:
    """
    Whisper invoca el ejecutable como `ffmpeg`, pero el binario distribuido por
    imageio_ffmpeg tiene nombre propio. Creamos un alias estable en %TEMP% para
    que Windows lo resuelva sin depender del PATH global.
    """
    source = Path(ffmpeg.get_ffmpeg_exe())
    alias_dir = Path(tempfile.gettempdir()) / "rrhh-ffmpeg"
    alias_dir.mkdir(parents=True, exist_ok=True)
    alias = alias_dir / "ffmpeg.exe"
    if not alias.exists():
        shutil.copy2(source, alias)
    os.environ["PATH"] = str(alias_dir) + os.pathsep + os.environ.get("PATH", "")
    return str(alias)

@lru_cache(maxsize=1)
def _get_whisper_model():
    try:
        import whisper
    except ModuleNotFoundError as exc:
        raise RuntimeError("Whisper no está instalado en este entorno.") from exc
    model_name = getattr(settings, "WHISPER_MODEL", "base")
    return whisper.load_model(model_name)

class QuestionOption(BaseModel):
    label: str  # A, B, C, D
    text: str

class QuestionSchema(BaseModel):
    text: str
    question_type: str  # "scale", "boolean", "multiple", "open"
    options: Optional[List[QuestionOption]] = None

class FormSchema(BaseModel):
    questions: List[QuestionSchema]

def transcribe_video(video_path: str) -> str:
    """Extracts audio and transcribes the video using Whisper."""
    _ensure_ffmpeg_alias()
    result = _get_whisper_model().transcribe(video_path)
    return result["text"]

def generate_questions_from_text(transcript: str, num_questions: int, question_types: List[str], skills: str) -> List[dict]:
    """Uses Gemini to generate questions based on the transcript and skills."""
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise ValueError("GEMINI_API_KEY no configurado")

    client = genai.Client(api_key=api_key)
    
    prompt = f"""
Has recibido la transcripción de un video. Tu tarea es extraer los puntos más importantes relacionados con las siguientes habilidades/temas (skills): {skills}.
A partir de estos puntos, debes generar {num_questions} preguntas para un formulario de evaluación.
Los tipos de preguntas permitidos son: {', '.join(question_types)}.

Tipos válidos a usar en el JSON para 'question_type': "scale" (1 a 5), "boolean" (Verdadero/Falso), "multiple" (Opciones A/B/C/D), "open" (Pregunta abierta).
Si el tipo es "multiple", proporciona un arreglo en 'options' con objetos que contengan 'label' (A, B, C, D) y 'text' (la opción en sí). Para los demás, 'options' puede ser null.

Transcripción del video:
{transcript}
    """

    response = client.models.generate_content(
        model='gemini-2.5-flash',
        contents=prompt,
        config=genai.types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=FormSchema,
            temperature=0.3,
        ),
    )
    
    parsed = getattr(response, "parsed", None)
    if parsed and getattr(parsed, "questions", None):
        return [q.model_dump() if hasattr(q, "model_dump") else q for q in parsed.questions]

    try:
        data = json.loads(response.text)
        return data.get("questions", [])
    except json.JSONDecodeError:
        return []

def process_video_and_generate_form(video_file, num_questions: int, question_types: List[str], skills: str) -> List[dict]:
    """Main flow to save video temp, transcribe, and generate questions."""
    # Guardar video temporalmente
    with tempfile.NamedTemporaryFile(delete=False, suffix=".mp4") as tmp:
        for chunk in video_file.chunks():
            tmp.write(chunk)
        tmp_path = tmp.name

    try:
        transcript = transcribe_video(tmp_path)
        questions = generate_questions_from_text(transcript, num_questions, question_types, skills)
        return questions
    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
