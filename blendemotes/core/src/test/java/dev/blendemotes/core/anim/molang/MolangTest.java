package dev.blendemotes.core.anim.molang;

import dev.blendemotes.core.TestRunner;

public class MolangTest {
    public void testArithmetic() {
        TestRunner.near(7, Molang.parse("1 + 2 * 3").eval(Molang.ZERO_CONTEXT), 1e-12, "precedence");
        TestRunner.near(9, Molang.parse("(1 + 2) * 3").eval(Molang.ZERO_CONTEXT), 1e-12, "parens");
        TestRunner.near(-4, Molang.parse("-2 * 2").eval(Molang.ZERO_CONTEXT), 1e-12, "unary");
        TestRunner.near(1, Molang.parse("math.sin(90)").eval(Molang.ZERO_CONTEXT), 1e-12, "degrees");
        TestRunner.near(5, Molang.parse("math.clamp(10, 0, 5)").eval(Molang.ZERO_CONTEXT), 1e-12, "clamp");
        TestRunner.near(2, Molang.parse("1 > 0 ? 2 : 3").eval(Molang.ZERO_CONTEXT), 1e-12, "ternary");
        TestRunner.check(Molang.parse("1+1").isConstant(), "constant folding");
    }

    public void testQueries() {
        Molang.Expr e = Molang.parse("math.sin(q.anim_time * 90) * 10");
        TestRunner.check(!e.isConstant(), "depends on time");
        TestRunner.check(e instanceof Molang.Sourced, "keeps source");
        Molang.Context ctx = new Molang.Context() {
            public double animTime() {
                return 1;
            }

            public double lifeTime() {
                return 0;
            }
        };
        TestRunner.near(10, e.eval(ctx), 1e-9, "anim_time");
        TestRunner.near(0, Molang.parse("variable.unknown").eval(Molang.ZERO_CONTEXT), 0, "unknown -> 0");
        try {
            Molang.parse("1 + ");
            throw new AssertionError("accepted bad molang");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
