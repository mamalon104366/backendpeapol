package dev.blendemotes.core.anim.molang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tiny Molang subset used by Blockbench/GeckoLib animations: numbers, {@code + - * / %},
 * comparisons, {@code ?:}, {@code && ||}, parentheses, {@code math.*} functions and a few
 * {@code query.*} values. Unknown identifiers evaluate to 0 (like Bedrock does).
 * <p>
 * Trigonometric functions use degrees, as in Bedrock.
 */
public final class Molang {
    private Molang() {
    }

    /** Evaluation context. */
    public interface Context {
        /** Seconds since the animation started. */
        double animTime();

        /** Seconds the entity has existed (used for idle style expressions). */
        double lifeTime();
    }

    public static final Context ZERO_CONTEXT = new Context() {
        @Override
        public double animTime() {
            return 0;
        }

        @Override
        public double lifeTime() {
            return 0;
        }
    };

    /** Compiled expression. */
    public interface Expr {
        double eval(Context ctx);

        /** True when the value does not depend on the context. */
        boolean isConstant();
    }

    private static final class Const implements Expr {
        final double value;

        Const(double value) {
            this.value = value;
        }

        @Override
        public double eval(Context ctx) {
            return value;
        }

        @Override
        public boolean isConstant() {
            return true;
        }
    }

    public static Expr constant(double value) {
        return new Const(value);
    }

    /**
     * Parses a Molang expression. Statements separated by {@code ;} are supported only in the
     * trivial "return x;" form. Throws {@link IllegalArgumentException} on syntax errors.
     */
    public static Expr parse(String source) {
        String src = source.trim();
        if (src.endsWith(";")) {
            src = src.substring(0, src.length() - 1).trim();
        }
        if (src.toLowerCase(Locale.ROOT).startsWith("return ")) {
            src = src.substring(7).trim();
        }
        Parser p = new Parser(src);
        Expr e = p.ternary();
        p.skipWs();
        if (p.pos != p.src.length()) {
            throw new IllegalArgumentException("Unexpected '" + p.src.charAt(p.pos) + "' in molang: " + source);
        }
        if (e.isConstant() && !(e instanceof Const)) {
            return new Const(e.eval(ZERO_CONTEXT));
        }
        return e;
    }

    // ------------------------------------------------------------------ parser

    private static final class Parser {
        final String src;
        int pos;

        Parser(String src) {
            this.src = src;
        }

        void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        boolean eat(String token) {
            skipWs();
            if (src.startsWith(token, pos)) {
                pos += token.length();
                return true;
            }
            return false;
        }

        Expr ternary() {
            Expr cond = or();
            if (eat("?")) {
                Expr a = ternary();
                if (!eat(":")) {
                    throw new IllegalArgumentException("Expected ':' in molang: " + src);
                }
                Expr b = ternary();
                return fold(new Ternary(cond, a, b));
            }
            return cond;
        }

        Expr or() {
            Expr left = and();
            while (eat("||")) {
                left = fold(new Binary('|', left, and()));
            }
            return left;
        }

        Expr and() {
            Expr left = comparison();
            while (eat("&&")) {
                left = fold(new Binary('&', left, comparison()));
            }
            return left;
        }

        Expr comparison() {
            Expr left = additive();
            while (true) {
                if (eat("<=")) {
                    left = fold(new Binary('l', left, additive()));
                } else if (eat(">=")) {
                    left = fold(new Binary('g', left, additive()));
                } else if (eat("==")) {
                    left = fold(new Binary('=', left, additive()));
                } else if (eat("!=")) {
                    left = fold(new Binary('!', left, additive()));
                } else if (eat("<")) {
                    left = fold(new Binary('<', left, additive()));
                } else if (eat(">")) {
                    left = fold(new Binary('>', left, additive()));
                } else {
                    return left;
                }
            }
        }

        Expr additive() {
            Expr left = multiplicative();
            while (true) {
                if (eat("+")) {
                    left = fold(new Binary('+', left, multiplicative()));
                } else if (eat("-")) {
                    left = fold(new Binary('-', left, multiplicative()));
                } else {
                    return left;
                }
            }
        }

        Expr multiplicative() {
            Expr left = unary();
            while (true) {
                if (eat("*")) {
                    left = fold(new Binary('*', left, unary()));
                } else if (eat("/")) {
                    left = fold(new Binary('/', left, unary()));
                } else if (eat("%")) {
                    left = fold(new Binary('%', left, unary()));
                } else {
                    return left;
                }
            }
        }

        Expr unary() {
            if (eat("-")) {
                return fold(new Binary('-', new Const(0), unary()));
            }
            if (eat("+")) {
                return unary();
            }
            if (eat("!")) {
                return fold(new Binary('=', new Const(0), unary()));
            }
            return primary();
        }

        Expr primary() {
            skipWs();
            if (pos >= src.length()) {
                throw new IllegalArgumentException("Unexpected end of molang: " + src);
            }
            char c = src.charAt(pos);
            if (c == '(') {
                pos++;
                Expr e = ternary();
                if (!eat(")")) {
                    throw new IllegalArgumentException("Expected ')' in molang: " + src);
                }
                return e;
            }
            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.'
                        || src.charAt(pos) == 'e' || src.charAt(pos) == 'E'
                        || ((src.charAt(pos) == '-' || src.charAt(pos) == '+') && pos > start
                        && (src.charAt(pos - 1) == 'e' || src.charAt(pos - 1) == 'E')))) {
                    pos++;
                }
                if (pos < src.length() && (src.charAt(pos) == 'f' || src.charAt(pos) == 'F')) {
                    pos++;
                    return new Const(Double.parseDouble(src.substring(start, pos - 1)));
                }
                return new Const(Double.parseDouble(src.substring(start, pos)));
            }
            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_' || src.charAt(pos) == '.')) {
                    pos++;
                }
                String name = src.substring(start, pos).toLowerCase(Locale.ROOT);
                List<Expr> args = new ArrayList<Expr>();
                if (eat("(")) {
                    if (!eat(")")) {
                        do {
                            args.add(ternary());
                        } while (eat(","));
                        if (!eat(")")) {
                            throw new IllegalArgumentException("Expected ')' after arguments in molang: " + src);
                        }
                    }
                }
                return identifier(name, args);
            }
            throw new IllegalArgumentException("Unexpected '" + c + "' in molang: " + src);
        }

        Expr identifier(String name, List<Expr> args) {
            if (name.startsWith("m.")) {
                name = "math." + name.substring(2);
            } else if (name.startsWith("q.")) {
                name = "query." + name.substring(2);
            }
            if (name.equals("math.pi")) {
                return new Const(Math.PI);
            }
            if (name.equals("query.anim_time")) {
                return new Query(true);
            }
            if (name.equals("query.life_time")) {
                return new Query(false);
            }
            if (name.startsWith("math.")) {
                Function f = FUNCTIONS.get(name.substring(5));
                if (f != null) {
                    return fold(new Call(f, args.toArray(new Expr[0])));
                }
            }
            return new Const(0);
        }
    }

    private static Expr fold(Expr e) {
        if (e.isConstant() && !(e instanceof Const)) {
            return new Const(e.eval(ZERO_CONTEXT));
        }
        return e;
    }

    // ------------------------------------------------------------------ nodes

    private static final class Query implements Expr {
        final boolean anim;

        Query(boolean anim) {
            this.anim = anim;
        }

        @Override
        public double eval(Context ctx) {
            return anim ? ctx.animTime() : ctx.lifeTime();
        }

        @Override
        public boolean isConstant() {
            return false;
        }
    }

    private static final class Binary implements Expr {
        final char op;
        final Expr a;
        final Expr b;

        Binary(char op, Expr a, Expr b) {
            this.op = op;
            this.a = a;
            this.b = b;
        }

        @Override
        public double eval(Context ctx) {
            double x = a.eval(ctx);
            double y = b.eval(ctx);
            switch (op) {
                case '+': return x + y;
                case '-': return x - y;
                case '*': return x * y;
                case '/': return y == 0 ? 0 : x / y;
                case '%': return y == 0 ? 0 : x % y;
                case '<': return x < y ? 1 : 0;
                case '>': return x > y ? 1 : 0;
                case 'l': return x <= y ? 1 : 0;
                case 'g': return x >= y ? 1 : 0;
                case '=': return x == y ? 1 : 0;
                case '!': return x != y ? 1 : 0;
                case '&': return (x != 0 && y != 0) ? 1 : 0;
                case '|': return (x != 0 || y != 0) ? 1 : 0;
                default: return 0;
            }
        }

        @Override
        public boolean isConstant() {
            return a.isConstant() && b.isConstant();
        }
    }

    private static final class Ternary implements Expr {
        final Expr cond;
        final Expr a;
        final Expr b;

        Ternary(Expr cond, Expr a, Expr b) {
            this.cond = cond;
            this.a = a;
            this.b = b;
        }

        @Override
        public double eval(Context ctx) {
            return cond.eval(ctx) != 0 ? a.eval(ctx) : b.eval(ctx);
        }

        @Override
        public boolean isConstant() {
            return cond.isConstant() && a.isConstant() && b.isConstant();
        }
    }

    private interface Function {
        double apply(double[] args);
    }

    private static final class Call implements Expr {
        final Function f;
        final Expr[] args;

        Call(Function f, Expr[] args) {
            this.f = f;
            this.args = args;
        }

        @Override
        public double eval(Context ctx) {
            double[] v = new double[args.length];
            for (int i = 0; i < args.length; i++) {
                v[i] = args[i].eval(ctx);
            }
            return f.apply(v);
        }

        @Override
        public boolean isConstant() {
            for (Expr e : args) {
                if (!e.isConstant()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static double arg(double[] a, int i) {
        return i < a.length ? a[i] : 0;
    }

    private static final Map<String, Function> FUNCTIONS = new HashMap<String, Function>();

    static {
        FUNCTIONS.put("sin", new Function() {
            public double apply(double[] a) {
                return Math.sin(Math.toRadians(arg(a, 0)));
            }
        });
        FUNCTIONS.put("cos", new Function() {
            public double apply(double[] a) {
                return Math.cos(Math.toRadians(arg(a, 0)));
            }
        });
        FUNCTIONS.put("abs", new Function() {
            public double apply(double[] a) {
                return Math.abs(arg(a, 0));
            }
        });
        FUNCTIONS.put("sqrt", new Function() {
            public double apply(double[] a) {
                return Math.sqrt(Math.max(0, arg(a, 0)));
            }
        });
        FUNCTIONS.put("pow", new Function() {
            public double apply(double[] a) {
                return Math.pow(arg(a, 0), arg(a, 1));
            }
        });
        FUNCTIONS.put("min", new Function() {
            public double apply(double[] a) {
                return Math.min(arg(a, 0), arg(a, 1));
            }
        });
        FUNCTIONS.put("max", new Function() {
            public double apply(double[] a) {
                return Math.max(arg(a, 0), arg(a, 1));
            }
        });
        FUNCTIONS.put("clamp", new Function() {
            public double apply(double[] a) {
                return Math.max(arg(a, 1), Math.min(arg(a, 2), arg(a, 0)));
            }
        });
        FUNCTIONS.put("lerp", new Function() {
            public double apply(double[] a) {
                return arg(a, 0) + (arg(a, 1) - arg(a, 0)) * arg(a, 2);
            }
        });
        FUNCTIONS.put("floor", new Function() {
            public double apply(double[] a) {
                return Math.floor(arg(a, 0));
            }
        });
        FUNCTIONS.put("ceil", new Function() {
            public double apply(double[] a) {
                return Math.ceil(arg(a, 0));
            }
        });
        FUNCTIONS.put("round", new Function() {
            public double apply(double[] a) {
                return Math.round(arg(a, 0));
            }
        });
        FUNCTIONS.put("trunc", new Function() {
            public double apply(double[] a) {
                double v = arg(a, 0);
                return v < 0 ? Math.ceil(v) : Math.floor(v);
            }
        });
        FUNCTIONS.put("mod", new Function() {
            public double apply(double[] a) {
                return arg(a, 1) == 0 ? 0 : arg(a, 0) % arg(a, 1);
            }
        });
        FUNCTIONS.put("exp", new Function() {
            public double apply(double[] a) {
                return Math.exp(arg(a, 0));
            }
        });
        FUNCTIONS.put("ln", new Function() {
            public double apply(double[] a) {
                return arg(a, 0) <= 0 ? 0 : Math.log(arg(a, 0));
            }
        });
        FUNCTIONS.put("asin", new Function() {
            public double apply(double[] a) {
                return Math.toDegrees(Math.asin(arg(a, 0)));
            }
        });
        FUNCTIONS.put("acos", new Function() {
            public double apply(double[] a) {
                return Math.toDegrees(Math.acos(arg(a, 0)));
            }
        });
        FUNCTIONS.put("atan", new Function() {
            public double apply(double[] a) {
                return Math.toDegrees(Math.atan(arg(a, 0)));
            }
        });
        FUNCTIONS.put("atan2", new Function() {
            public double apply(double[] a) {
                return Math.toDegrees(Math.atan2(arg(a, 0), arg(a, 1)));
            }
        });
        FUNCTIONS.put("sign", new Function() {
            public double apply(double[] a) {
                return Math.signum(arg(a, 0));
            }
        });
        FUNCTIONS.put("to_rad", new Function() {
            public double apply(double[] a) {
                return Math.toRadians(arg(a, 0));
            }
        });
        FUNCTIONS.put("to_deg", new Function() {
            public double apply(double[] a) {
                return Math.toDegrees(arg(a, 0));
            }
        });
        FUNCTIONS.put("hermite_blend", new Function() {
            public double apply(double[] a) {
                double t = arg(a, 0);
                return 3 * t * t - 2 * t * t * t;
            }
        });
    }
}
