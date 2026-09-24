package dev.blendemotes.core.json;

import dev.blendemotes.core.TestRunner;

import java.util.List;
import java.util.Map;

public class JsonTest {
    public void testRoundTrip() {
        String src = "{\"a\": [1, 2.5, -3e2, true, false, null, \"x\\n\\u00e9\"], \"b\": {\"c\": {}}, \"d\": []}";
        Object v = Json.parse(src);
        Map<String, Object> o = JsonUtil.asObject(v, "root");
        List<Object> a = JsonUtil.asArray(o.get("a"), "a");
        TestRunner.near(1, ((Number) a.get(0)).doubleValue(), 0, "int");
        TestRunner.near(2.5, ((Number) a.get(1)).doubleValue(), 0, "double");
        TestRunner.near(-300, ((Number) a.get(2)).doubleValue(), 0, "exponent");
        TestRunner.check(a.get(5) == Json.NULL, "null");
        TestRunner.check("x\né".equals(a.get(6)), "escapes");
        Object again = Json.parse(Json.write(v));
        TestRunner.check(Json.write(again).equals(Json.write(v)), "write/parse round trip");
        TestRunner.check(Json.writePretty(v).contains("\n"), "pretty");
    }

    public void testLenientAndErrors() {
        Object v = Json.parse("﻿// comment\n{\"a\": [1, 2,], /* c */ \"b\": 1,}");
        TestRunner.check(v instanceof Map, "lenient parse");
        String[] bad = {"{", "[1 2]", "{\"a\" 1}", "tru", "\"abc", "{} x", "-"};
        for (String b : bad) {
            try {
                Json.parse(b);
                throw new AssertionError("accepted bad json: " + b);
            } catch (JsonException expected) {
                TestRunner.check(expected.getMessage().contains("line"), "error has position");
            }
        }
    }
}
