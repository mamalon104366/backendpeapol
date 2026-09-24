package dev.blendemotes.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependency free test runner (the build must work without downloading JUnit).
 * Runs every public no-arg {@code test*} method of the given classes.
 */
public final class TestRunner {
    private TestRunner() {
    }

    public static void main(String[] args) throws Exception {
        String[] classes = args.length > 0 ? args : new String[]{
                "dev.blendemotes.core.json.JsonTest",
                "dev.blendemotes.core.anim.TrackTest",
                "dev.blendemotes.core.anim.molang.MolangTest",
                "dev.blendemotes.core.pose.BlenderGroundTruthTest",
                "dev.blendemotes.core.pose.PoseTest",
                "dev.blendemotes.core.bend.BendMeshTest",
                "dev.blendemotes.core.net.EmoteCodecTest",
                "dev.blendemotes.core.emote.EmoteLibraryTest",
                "dev.blendemotes.core.emote.EmotePlayerTest",
                "dev.blendemotes.core.anim.io.LegacyLoaderTest",
        };
        int passed = 0;
        List<String> failures = new ArrayList<String>();
        for (String name : classes) {
            Class<?> cls;
            try {
                cls = Class.forName(name);
            } catch (ClassNotFoundException ex) {
                failures.add(name + ": class not found");
                continue;
            }
            for (Method m : cls.getDeclaredMethods()) {
                if (!m.getName().startsWith("test") || m.getParameterTypes().length != 0 || !Modifier.isPublic(m.getModifiers())) {
                    continue;
                }
                Object instance = Modifier.isStatic(m.getModifiers()) ? null : cls.getDeclaredConstructor().newInstance();
                long start = System.nanoTime();
                try {
                    m.invoke(instance);
                    passed++;
                    System.out.printf("  PASS %s.%s (%d ms)%n", cls.getSimpleName(), m.getName(), (System.nanoTime() - start) / 1000000);
                } catch (InvocationTargetException ex) {
                    Throwable cause = ex.getCause();
                    failures.add(cls.getSimpleName() + "." + m.getName() + ": " + cause);
                    System.out.printf("  FAIL %s.%s: %s%n", cls.getSimpleName(), m.getName(), cause);
                    cause.printStackTrace(System.out);
                }
            }
        }
        System.out.println();
        System.out.println(passed + " passed, " + failures.size() + " failed");
        for (String f : failures) {
            System.out.println("  - " + f);
        }
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ assertions

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void near(double expected, double actual, double eps, String message) {
        if (Double.isNaN(actual) || Math.abs(expected - actual) > eps) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual + " (eps " + eps + ")");
        }
    }

    public static String resource(String path) {
        InputStream in = TestRunner.class.getResourceAsStream(path);
        if (in == null) {
            throw new AssertionError("missing test resource " + path);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            in.close();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }
}
