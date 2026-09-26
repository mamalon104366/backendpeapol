// Construye rutas a /public respetando la base de Vite (funciona en subcarpetas).
export const asset = (path) => `${import.meta.env.BASE_URL}${path.replace(/^\//, '')}`;
