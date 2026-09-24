package dev.blendemotes.core.anim.io;

import dev.blendemotes.core.anim.BoneAnimation;
import dev.blendemotes.core.anim.Easing;
import dev.blendemotes.core.anim.Keyframe;
import dev.blendemotes.core.anim.Track;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Repairs Bezier handles written by the export script embedded in {@code emote_creator.blend}
 * (rig 2.0). Comparing the exported files against Blender's own evaluation showed two defects
 * in that script ({@code set_up_bedrock.py -> get_bezier_args}):
 * <ol>
 *     <li>handle values are not multiplied by the axis sign, so every channel whose bone axis
 *     points the opposite way of the Minecraft axis (head/waist Y and Z rotation, arm and leg
 *     Y position...) gets handles pointing the wrong way;</li>
 *     <li>handles of the {@code bend} channel are left in radians while the values are in
 *     degrees.</li>
 * </ol>
 * Instead of hard coding which channels are affected (the rig may change), each track is
 * checked: Blender's handles follow the direction of the curve, so a track whose handles
 * consistently point against it is flipped, and a bend track whose handles are ~57 times too
 * flat is converted to degrees. Files that do not show these symptoms are left untouched.
 */
public final class RigExportRepair {
    private RigExportRepair() {
    }

    /** Frame rates tried when snapping rounded key times back onto Blender's frame grid. */
    private static final double[] FRAME_RATES = {24, 30, 25, 60, 20, 50, 48, 12, 15, 120};
    /** The rig writes times rounded to 3 decimals. */
    private static final double TIME_ROUNDING = 0.00051;

    /** Result counters, handy for logs and tests. */
    public static final class Report {
        public int flippedTracks;
        public int radianTracks;
        /** Frame rate the key times were snapped to, 0 when not snapped. */
        public double snappedFps;

        @Override
        public String toString() {
            return flippedTracks + " flipped, " + radianTracks + " converted from radians, fps " + snappedFps;
        }
    }

    /**
     * Finds the frame rate the animation was made with: every key time must be a whole frame
     * once the exporter's rounding is undone. Returns 0 when no frame rate fits.
     */
    public static double detectFrameRate(java.util.Collection<BoneAnimation> bones) {
        List<Double> times = new ArrayList<Double>();
        for (BoneAnimation b : bones) {
            for (int a = 0; a < 3; a++) {
                collect(b.position[a], times);
                collect(b.rotation[a], times);
                collect(b.scale[a], times);
            }
            collect(b.bend, times);
        }
        if (times.size() < 2) {
            return 0;
        }
        for (double fps : FRAME_RATES) {
            boolean ok = true;
            for (double t : times) {
                double frames = t * fps;
                if (Math.abs(frames - Math.rint(frames)) / fps > TIME_ROUNDING) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return fps;
            }
        }
        return 0;
    }

    private static void collect(Track t, List<Double> out) {
        for (int i = 0; i < t.size(); i++) {
            out.add(t.get(i).time);
        }
    }

    /** Rounds a time to the nearest frame of {@code fps}. */
    public static double snap(double time, double fps) {
        return fps <= 0 ? time : Math.rint(time * fps) / fps;
    }

    /** Moves every key onto the exact frame time (undoes the exporter's 3-decimal rounding). */
    public static BoneAnimation snapTimes(BoneAnimation bone, double fps) {
        Track[] pos = new Track[3];
        Track[] rot = new Track[3];
        Track[] scale = new Track[3];
        for (int a = 0; a < 3; a++) {
            pos[a] = snapTrack(bone.position[a], fps);
            rot[a] = snapTrack(bone.rotation[a], fps);
            scale[a] = snapTrack(bone.scale[a], fps);
        }
        return new BoneAnimation(pos, rot, scale, snapTrack(bone.bend, fps));
    }

    private static Track snapTrack(Track track, double fps) {
        if (track.isEmpty()) {
            return track;
        }
        List<Keyframe> out = new ArrayList<Keyframe>(track.size());
        for (int i = 0; i < track.size(); i++) {
            Keyframe k = track.get(i);
            out.add(k.withTime(snap(k.time, fps)));
        }
        return new Track(out);
    }

    public static BoneAnimation repair(BoneAnimation bone, Report report) {
        Track[] pos = new Track[3];
        Track[] rot = new Track[3];
        Track[] scale = new Track[3];
        for (int a = 0; a < 3; a++) {
            pos[a] = repairTrack(bone.position[a], false, report);
            rot[a] = repairTrack(bone.rotation[a], false, report);
            scale[a] = repairTrack(bone.scale[a], false, report);
        }
        Track bend = repairTrack(bone.bend, true, report);
        return new BoneAnimation(pos, rot, scale, bend);
    }

    static Track repairTrack(Track track, boolean bend, Report report) {
        if (track.size() < 2) {
            return track;
        }
        int agree = 0;
        int disagree = 0;
        List<Double> ratios = new ArrayList<Double>();
        for (int i = 0; i < track.size(); i++) {
            Keyframe k = track.get(i);
            if (!k.isConstant() || (k.easing != Easing.BEZIER)) {
                continue;
            }
            double v = k.value.eval(null);
            if (k.hasRightHandle && i + 1 < track.size() && k.rightDt > 1e-9 && track.get(i + 1).isConstant()) {
                Keyframe n = track.get(i + 1);
                double secant = (n.value.eval(null) - v) / (n.time - k.time);
                double slope = k.rightDv / k.rightDt;
                if (Math.abs(secant) > 1e-6 && Math.abs(slope) > 1e-9) {
                    if (slope * secant > 0) {
                        agree++;
                    } else {
                        disagree++;
                    }
                    ratios.add(Math.abs(slope / secant));
                }
            }
            if (k.hasLeftHandle && i > 0 && k.leftDt < -1e-9 && track.get(i - 1).isConstant()) {
                Keyframe p = track.get(i - 1);
                double secant = (v - p.value.eval(null)) / (k.time - p.time);
                double slope = k.leftDv / k.leftDt;
                if (Math.abs(secant) > 1e-6 && Math.abs(slope) > 1e-9) {
                    if (slope * secant > 0) {
                        agree++;
                    } else {
                        disagree++;
                    }
                    ratios.add(Math.abs(slope / secant));
                }
            }
        }
        double factor = 1;
        if (disagree >= 2 && disagree >= 4 * agree) {
            factor = -1;
            report.flippedTracks++;
        }
        if (bend && ratios.size() >= 2) {
            Double[] r = ratios.toArray(new Double[0]);
            Arrays.sort(r);
            double median = r[r.length / 2];
            if (median < 0.1) {
                factor *= Math.toDegrees(1);
                report.radianTracks++;
            }
        }
        if (factor == 1) {
            return track;
        }
        List<Keyframe> out = new ArrayList<Keyframe>(track.size());
        for (int i = 0; i < track.size(); i++) {
            Keyframe k = track.get(i);
            if (k.easing == Easing.BEZIER) {
                k = new Keyframe(k.time, k.value, k.easing, k.easingArg,
                        k.hasLeftHandle, k.leftDt, k.leftDv * factor,
                        k.hasRightHandle, k.rightDt, k.rightDv * factor);
            }
            out.add(k);
        }
        return new Track(out);
    }
}
