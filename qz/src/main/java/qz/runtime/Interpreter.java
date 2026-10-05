package qz.runtime;

import qxz.ast.Ast;
import qz.runtime.ui.DefaultUiHost;
import qz.runtime.ui.UiHost;
import qz.runtime.ui.UiLayoutEngine;
import qz.runtime.ui.UiNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QZ 运行时解释器。
 * 负责执行 QXZ 前端生成的 AST，包含表达式求值、语句执行、
 * 函数调用、作用域管理以及标准库函数（print、len、range 等）。
 */
public class Interpreter {

    // 标准库函数注册
    public interface QzNative {
        Object call(List<Object> args);
    }

    private final Environment globals = new Environment();
    private Environment env = globals;
    private final java.util.Scanner scanner = new java.util.Scanner(System.in);

    /** QZ 语言解析器注册表：Java / C# / C++ 统一语法体系由 QZ 注册执行 */
    private final LanguageRegistry registry = new LanguageRegistry();
    /** QZ JVM 宿主：鸿蒙 5+ 无系统级 JVM，由 QZ 装载（行级标记附加内容 //Java 25 JVM 25） */
    private final JvmHost jvmHost = new JvmHost();
    /** 当前激活的语言上下文（由行级语言标记驱动；null 表示未指定） */
    private String currentLanguage = null;

    /** QZ UI 宿主：默认控制台调试输出，安卓壳等平台可 setUiHost 接管绘制 */
    private UiHost uiHost = new DefaultUiHost();
    /** 已注册的 ui 屏幕（ui 块名 -> 运行时节点树） */
    private final Map<String, UiNode> uiScreens = new LinkedHashMap<>();

    public Interpreter() {
        registerStdlib();
    }

    public LanguageRegistry registry() { return registry; }
    public JvmHost jvmHost() { return jvmHost; }
    public String currentLanguage() { return currentLanguage; }
    public UiHost uiHost() { return uiHost; }
    public Map<String, UiNode> uiScreens() { return uiScreens; }

    /** 更换 UI 宿主（安卓壳适配时调用） */
    public void setUiHost(UiHost host) {
        this.uiHost = host != null ? host : new DefaultUiHost();
    }

    /** 触发布局并渲染指定屏幕；未注册则抛错 */
    public void showUi(String screenName) {
        UiNode root = uiScreens.get(screenName);
        if (root == null) {
            throw new QzRuntimeException("未注册的 UI 屏幕: " + screenName
                    + "（可用: " + uiScreens.keySet() + "）");
        }
        UiLayoutEngine.LayoutResult result = new UiLayoutEngine().layout(root);
        uiHost.show(screenName, result.width, result.height, result.commands);
    }

    /** 将 AST ui 节点递归转换为运行时节点（属性求值） */
    private UiNode toRuntimeNode(Ast.UiNode node) {
        Map<String, Object> props = new LinkedHashMap<>();
        if (node.props != null) {
            for (Map.Entry<String, Ast.Expr> e : node.props.entrySet()) {
                props.put(e.getKey(), evaluate(e.getValue()));
            }
        }
        List<UiNode> children = new ArrayList<>();
        if (node.children != null) {
            for (Ast.UiNode kid : node.children) children.add(toRuntimeNode(kid));
        }
        return new UiNode(node.type, node.name, props, children);
    }

    private void registerStdlib() {
        globals.define("print", (QzNative) args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(stringify(args.get(i)));
            }
            System.out.println(sb);
            return null;
        });

        globals.define("len", (QzNative) args -> {
            Object v = args.get(0);
            if (v instanceof List) return ((List<?>) v).size();
            if (v instanceof String) return ((String) v).length();
            throw new QzRuntimeException("len() 只支持列表和字符串");
        });

        globals.define("range", (QzNative) args -> {
            int start = 0, end, step = 1;
            if (args.size() == 1) {
                end = asInt(args.get(0));
            } else if (args.size() == 2) {
                start = asInt(args.get(0));
                end = asInt(args.get(1));
            } else if (args.size() == 3) {
                start = asInt(args.get(0));
                end = asInt(args.get(1));
                step = asInt(args.get(2));
            } else {
                throw new QzRuntimeException("range() 需要 1~3 个参数");
            }
            List<Object> result = new ArrayList<>();
            if (step > 0) {
                for (int i = start; i < end; i += step) result.add(i);
            } else {
                for (int i = start; i > end; i += step) result.add(i);
            }
            return result;
        });

        globals.define("type", (QzNative) args -> {
            Object v = args.get(0);
            if (v == null) return "null";
            if (v instanceof Boolean) return "boolean";
            if (v instanceof Integer) return "int";
            if (v instanceof Double) return "double";
            if (v instanceof String) return "string";
            if (v instanceof List) return "list";
            return v.getClass().getSimpleName();
        });

        globals.define("input", (QzNative) args -> {
            if (!args.isEmpty()) System.out.print(args.get(0));
            if (scanner.hasNextLine()) return scanner.nextLine();
            throw new QzRuntimeException("输入流已结束");
        });

        globals.define("toInt", (QzNative) args -> asInt(args.get(0)));
        globals.define("toDouble", (QzNative) args -> {
            Object v = args.get(0);
            if (v instanceof Integer) return ((Integer) v).doubleValue();
            if (v instanceof Double) return v;
            return Double.parseDouble(v.toString());
        });
        globals.define("toString", (QzNative) args -> stringify(args.get(0)));

        globals.define("abs", (QzNative) args -> {
            Object v = args.get(0);
            if (v instanceof Integer) return Math.abs((Integer) v);
            return Math.abs((Double) v);
        });
        globals.define("max", (QzNative) args -> Math.max(asInt(args.get(0)), asInt(args.get(1))));
        globals.define("min", (QzNative) args -> Math.min(asInt(args.get(0)), asInt(args.get(1))));
        globals.define("sqrt", (QzNative) args -> Math.sqrt(asDouble(args.get(0))));
        globals.define("floor", (QzNative) args -> (int) Math.floor(asDouble(args.get(0))));
        globals.define("ceil", (QzNative) args -> (int) Math.ceil(asDouble(args.get(0))));

        // UI 标准库
        globals.define("uiShow", (QzNative) args -> {
            if (args.size() < 1) throw new QzRuntimeException("uiShow() 需要屏幕名称参数");
            showUi(String.valueOf(args.get(0)));
            return null;
        });
        globals.define("uiScreens", (QzNative) args -> new ArrayList<>(uiScreens.keySet()));
        globals.define("uiRegister", (QzNative) args -> {
            if (args.size() < 2) throw new QzRuntimeException("uiRegister() 需要屏幕名与节点树");
            String screenName = String.valueOf(args.get(0));
            Object tree = args.get(1);
            UiNode root = fromConfigTree(screenName, tree);
            uiScreens.put(screenName, root);
            return null;
        });
    }

    /** 将 QXZ 配置块（Map）转换为运行时 UI 节点树，用于动态注册 */
    private UiNode fromConfigTree(String name, Object tree) {
        if (tree instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) tree;
            String type = map.containsKey("type") ? String.valueOf(map.get("type")) : "panel";
            Map<String, Object> props = new LinkedHashMap<>();
            List<UiNode> children = new ArrayList<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                Object val = e.getValue();
                if ("type".equals(key)) continue;
                if ("children".equals(key) && val instanceof List) {
                    List<?> kids = (List<?>) val;
                    for (int i = 0; i < kids.size(); i++) {
                        children.add(fromConfigTree(name + "#" + i, kids.get(i)));
                    }
                } else if (val instanceof Map) {
                    children.add(fromConfigTree(name + "." + key, val));
                } else {
                    props.put(key, val);
                }
            }
            return new UiNode(type, name, props, children);
        }
        throw new QzRuntimeException("uiRegister 节点必须是配置块（Map）");
    }

    private static int asInt(Object v) {
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Double) return ((Double) v).intValue();
        return Integer.parseInt(v.toString());
    }

    private static double asDouble(Object v) {
        if (v instanceof Double) return (Double) v;
        if (v instanceof Integer) return ((Integer) v).doubleValue();
        return Double.parseDouble(v.toString());
    }

    public static String stringify(Object v) {
        if (v == null) return "null";
        if (v instanceof Double) {
            double d = (Double) v;
            if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        if (v instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> list = (List<?>) v;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(stringify(list.get(i)));
            }
            return sb.append("]").toString();
        }
        if (v instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) v;
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(e.getKey()).append(": ").append(stringify(e.getValue()));
            }
            return sb.append("}").toString();
        }
        return String.valueOf(v);
    }

    // ================= 语句执行 =================

    public void execute(List<Ast.Stmt> statements) {
        try {
            for (Ast.Stmt stmt : statements) {
                executeStmt(stmt);
            }
        } catch (ReturnSignal sig) {
            throw new QzRuntimeException("return 语句只能出现在函数中");
        }
    }

    private static class ReturnSignal extends RuntimeException {
        final Object value;
        ReturnSignal(Object value) { super(null, null, false, false); this.value = value; }
    }

    private static class LoopControl extends RuntimeException {
        final boolean isBreak;
        LoopControl(boolean isBreak) { super(null, null, false, false); this.isBreak = isBreak; }
    }

    private void executeStmt(Ast.Stmt stmt) {
        if (stmt instanceof Ast.ExprStmt) {
            evaluate(((Ast.ExprStmt) stmt).expr);
        } else if (stmt instanceof Ast.VarDecl) {
            Ast.VarDecl vd = (Ast.VarDecl) stmt;
            env.define(vd.name, vd.initializer == null ? null : evaluate(vd.initializer));
        } else if (stmt instanceof Ast.Block) {
            executeBlock(((Ast.Block) stmt).statements, env.child());
        } else if (stmt instanceof Ast.If) {
            Ast.If ifStmt = (Ast.If) stmt;
            if (isTruthy(evaluate(ifStmt.condition))) {
                executeStmt(ifStmt.thenBranch);
            } else if (ifStmt.elseBranch != null) {
                executeStmt(ifStmt.elseBranch);
            }
        } else if (stmt instanceof Ast.While) {
            Ast.While w = (Ast.While) stmt;
            while (isTruthy(evaluate(w.condition))) {
                try {
                    executeStmt(w.body);
                } catch (LoopControl lc) {
                    if (lc.isBreak) break;
                }
            }
        } else if (stmt instanceof Ast.For) {
            Ast.For f = (Ast.For) stmt;
            Environment loopEnv = env.child();
            Environment saved = env;
            env = loopEnv;
            try {
                if (f.init != null) executeStmt(f.init);
                while (f.condition == null || isTruthy(evaluate(f.condition))) {
                    try {
                        executeStmt(f.body);
                    } catch (LoopControl lc) {
                        if (lc.isBreak) break;
                    }
                    if (f.increment != null) evaluate(f.increment);
                }
            } finally {
                env = saved;
            }
        } else if (stmt instanceof Ast.Break) {
            throw new LoopControl(true);
        } else if (stmt instanceof Ast.Continue) {
            throw new LoopControl(false);
        } else if (stmt instanceof Ast.FuncDecl) {
            Ast.FuncDecl fd = (Ast.FuncDecl) stmt;
            env.define(fd.name, new QxzFunction(fd.name, fd.params, fd.body, env));
        } else if (stmt instanceof Ast.Return) {
            Ast.Return r = (Ast.Return) stmt;
            throw new ReturnSignal(r.value == null ? null : evaluate(r.value));
        } else if (stmt instanceof Ast.Section) {
            Ast.Section s = (Ast.Section) stmt;
            Map<String, Object> sectionMap = new HashMap<>();
            for (Ast.SectionEntry entry : s.entries) {
                sectionMap.put(entry.key, evaluate(entry.value));
            }
            env.define(s.name, sectionMap);
        } else if (stmt instanceof Ast.UiDecl) {
            // ui 块：转换为运行时节点树注册，并立即布局渲染
            Ast.UiDecl ui = (Ast.UiDecl) stmt;
            Map<String, Object> rootProps = new LinkedHashMap<>();
            if (ui.rootProps != null) {
                for (Map.Entry<String, Ast.Expr> e : ui.rootProps.entrySet()) {
                    rootProps.put(e.getKey(), evaluate(e.getValue()));
                }
            }
            List<UiNode> roots = new ArrayList<>();
            for (Ast.UiNode node : ui.nodes) roots.add(toRuntimeNode(node));
            UiNode root = roots.size() == 1 && rootProps.isEmpty()
                    ? roots.get(0)
                    : new UiNode("panel", ui.name, rootProps, roots);
            uiScreens.put(ui.name, root);
            showUi(ui.name);
        } else if (stmt instanceof Ast.LangMarker) {
            // 行级语言标记：校验语言注册（Java/C#/C++ 统一语法体系），
            // 附加内容（如 JVM）由 QZ 作为宿主装载，而非依赖系统级 JVM
            Ast.LangMarker lm = (Ast.LangMarker) stmt;
            registry.activate(lm.marker, jvmHost);
            currentLanguage = lm.marker.language;
        } else if (stmt instanceof Ast.LangEnd) {
            // 结束标记：指定语言执行完成本行之后结束
            currentLanguage = null;
        } else {
            throw new QzRuntimeException("未知的语句类型: " + stmt.getClass().getSimpleName());
        }
    }

    public void executeBlock(List<Ast.Stmt> statements, Environment blockEnv) {
        Environment saved = env;
        try {
            env = blockEnv;
            for (Ast.Stmt stmt : statements) {
                executeStmt(stmt);
            }
        } finally {
            env = saved;
        }
    }

    // ================= 表达式求值 =================

    private Object evaluate(Ast.Expr expr) {
        if (expr instanceof Ast.Literal) {
            return ((Ast.Literal) expr).value;
        }
        if (expr instanceof Ast.Variable) {
            return env.get(((Ast.Variable) expr).name);
        }
        if (expr instanceof Ast.Unary) {
            Ast.Unary u = (Ast.Unary) expr;
            Object right = evaluate(u.right);
            switch (u.op) {
                case MINUS: return negate(right);
                case NOT: return !isTruthy(right);
                default: throw new QzRuntimeException("未知的一元运算符: " + u.op);
            }
        }
        if (expr instanceof Ast.Binary) {
            return evalBinary((Ast.Binary) expr);
        }
        if (expr instanceof Ast.Assign) {
            Ast.Assign a = (Ast.Assign) expr;
            Object value = evaluate(a.value);
            if (a.op == qxz.lexer.TokenType.ASSIGN) {
                env.assign(a.name, value);
                return value;
            }
            Object current = env.get(a.name);
            Object result = switch (a.op) {
                case PLUS_ASSIGN -> add(current, value);
                case MINUS_ASSIGN -> sub(current, value);
                case STAR_ASSIGN -> mul(current, value);
                case SLASH_ASSIGN -> div(current, value);
                default -> throw new QzRuntimeException("不支持的复合赋值: " + a.op);
            };
            env.assign(a.name, result);
            return result;
        }
        if (expr instanceof Ast.Call) {
            Ast.Call c = (Ast.Call) expr;
            Object callee = evaluate(c.callee);
            List<Object> args = new ArrayList<>();
            for (Ast.Expr arg : c.arguments) args.add(evaluate(arg));

            if (callee instanceof QxzFunction) {
                QxzFunction fn = (QxzFunction) callee;
                if (args.size() != fn.arity()) {
                    throw new QzRuntimeException("函数 " + fn.name + " 需要 " + fn.arity()
                            + " 个参数，实际传入 " + args.size());
                }
                Environment callEnv = fn.closure.child();
                for (int i = 0; i < fn.params.size(); i++) {
                    callEnv.define(fn.params.get(i), args.get(i));
                }
                Environment saved = env;
                try {
                    env = callEnv;
                    for (Ast.Stmt s : fn.body) executeStmt(s);
                } catch (ReturnSignal sig) {
                    return sig.value;
                } finally {
                    env = saved;
                }
                return null;
            }
            if (callee instanceof QzNative) {
                return ((QzNative) callee).call(args);
            }
            throw new QzRuntimeException("无法调用非函数值: " + stringify(callee));
        }
        if (expr instanceof Ast.ListLiteral) {
            List<Object> list = new ArrayList<>();
            for (Ast.Expr e : ((Ast.ListLiteral) expr).elements) list.add(evaluate(e));
            return list;
        }
        if (expr instanceof Ast.Index) {
            Ast.Index idx = (Ast.Index) expr;
            Object target = evaluate(idx.target);
            Object index = evaluate(idx.index);
            if (target instanceof List) {
                return ((List<?>) target).get(asInt(index));
            }
            if (target instanceof String) {
                String s = (String) target;
                int i = asInt(index);
                if (i < 0) i += s.length();
                return String.valueOf(s.charAt(i));
            }
            if (target instanceof Map) {
                return ((Map<?, ?>) target).get(index.toString());
            }
            throw new QzRuntimeException("下标访问只支持列表、字符串和配置块");
        }
        throw new QzRuntimeException("未知的表达式类型: " + expr.getClass().getSimpleName());
    }

    private Object evalBinary(Ast.Binary b) {
        // 短路求值
        if (b.op == qxz.lexer.TokenType.AND) {
            return isTruthy(evaluate(b.left)) && isTruthy(evaluate(b.right));
        }
        if (b.op == qxz.lexer.TokenType.OR) {
            return isTruthy(evaluate(b.left)) || isTruthy(evaluate(b.right));
        }
        Object left = evaluate(b.left);
        Object right = evaluate(b.right);
        return switch (b.op) {
            case PLUS -> add(left, right);
            case MINUS -> sub(left, right);
            case STAR -> mul(left, right);
            case SLASH -> div(left, right);
            case PERCENT -> mod(left, right);
            case EQ -> valuesEqual(left, right);
            case NEQ -> !valuesEqual(left, right);
            case LT -> compare(left, right) < 0;
            case LTE -> compare(left, right) <= 0;
            case GT -> compare(left, right) > 0;
            case GTE -> compare(left, right) >= 0;
            default -> throw new QzRuntimeException("未知的二元运算符: " + b.op);
        };
    }

    private Object add(Object a, Object b) {
        if (a instanceof String || b instanceof String) {
            return stringify(a) + stringify(b);
        }
        if (a instanceof Integer && b instanceof Integer) return (Integer) a + (Integer) b;
        if (a instanceof Number && b instanceof Number) return asDouble(a) + asDouble(b);
        if (a instanceof List && !(b instanceof List)) {
            List<Object> list = new ArrayList<>((List<?>) a);
            list.add(b);
            return list;
        }
        if (a instanceof List && b instanceof List) {
            List<Object> list = new ArrayList<>((List<?>) a);
            list.addAll((List<?>) b);
            return list;
        }
        throw new QzRuntimeException("无法对 " + stringify(a) + " 与 " + stringify(b) + " 执行加法");
    }

    private Object sub(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) return (Integer) a - (Integer) b;
        if (a instanceof Number && b instanceof Number) return asDouble(a) - asDouble(b);
        throw new QzRuntimeException("无法对 " + stringify(a) + " 与 " + stringify(b) + " 执行减法");
    }

    private Object mul(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) return (Integer) a * (Integer) b;
        if (a instanceof Number && b instanceof Number) return asDouble(a) * asDouble(b);
        if (a instanceof String && b instanceof Integer) {
            return ((String) a).repeat((Integer) b);
        }
        throw new QzRuntimeException("无法对 " + stringify(a) + " 与 " + stringify(b) + " 执行乘法");
    }

    private Object div(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            double result = asDouble(a) / asDouble(b);
            if (Double.isInfinite(result) || Double.isNaN(result)) {
                throw new QzRuntimeException("除数为零");
            }
            return result;
        }
        throw new QzRuntimeException("无法对 " + stringify(a) + " 与 " + stringify(b) + " 执行除法");
    }

    private Object mod(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) {
            if ((Integer) b == 0) throw new QzRuntimeException("除数为零");
            return (Integer) a % (Integer) b;
        }
        return asDouble(a) % asDouble(b);
    }

    private Object negate(Object v) {
        if (v instanceof Integer) return -(Integer) v;
        if (v instanceof Double) return -(Double) v;
        throw new QzRuntimeException("无法对 " + stringify(v) + " 取负");
    }

    private int compare(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(asDouble(a), asDouble(b));
        }
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b);
        }
        throw new QzRuntimeException("无法比较 " + stringify(a) + " 与 " + stringify(b));
    }

    private boolean valuesEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        if (a instanceof Number && b instanceof Number) {
            return asDouble(a) == asDouble(b);
        }
        return a.equals(b);
    }

    private boolean isTruthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        return true;
    }
}
