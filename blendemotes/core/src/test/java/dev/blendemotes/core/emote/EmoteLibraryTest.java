package dev.blendemotes.core.emote;

import dev.blendemotes.core.TestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EmoteLibraryTest {
    static EmoteLibrary.Resource resource(final String name) {
        return new EmoteLibrary.Resource() {
            public String name() {
                return name;
            }

            public InputStream open() throws IOException {
                return new ByteArrayInputStream(TestRunner.resource("/blender/" + name).getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    public void testLoadsBuiltinAndFolder() throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"), "blendemotes-test-" + System.nanoTime());
        TestRunner.check(dir.mkdirs(), "tmp dir");
        FileOutputStream out = new FileOutputStream(new File(dir, "broken.json"));
        out.write("{ not json".getBytes(StandardCharsets.UTF_8));
        out.close();
        out = new FileOutputStream(new File(dir, "copy.json"));
        out.write(TestRunner.resource("/blender/inchworm.json").getBytes(StandardCharsets.UTF_8));
        out.close();
        List<EmoteLibrary.Resource> builtin = new ArrayList<EmoteLibrary.Resource>();
        builtin.add(resource("cartwheel.json"));
        builtin.add(resource("inchworm.json"));
        EmoteLibrary lib = new EmoteLibrary();
        lib.reload(builtin, dir);
        TestRunner.check(lib.all().size() == 2, "same content -> same id, no duplicates: " + lib.all().size());
        TestRunner.check(lib.errors().containsKey("broken.json"), "broken file reported");
        Emote cart = lib.findByName("Cartwheel");
        TestRunner.check(cart != null && cart.info.icon != null && cart.info.icon.length > 1000, "icon decoded");
        TestRunner.check("3APA3EH".equals(cart.info.author), "author");
        TestRunner.check(cart.info.badges.size() == 1, "badges ('bages') read");
        for (File f : dir.listFiles()) {
            f.delete();
        }
        dir.delete();
    }
}
