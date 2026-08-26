package qxz.ast;

import java.util.List;

/**
 * QXZ 抽象语法树（AST）节点定义。
 * 前端（qxz-core）解析出 AST 后，交给后端（QZ 运行时）执行。
 */
public class Ast {

    // ================= 表达式 =================

    public interface Expr { }

    /** 字面量：数字/字符串/布尔/null */
    public static class Literal implements Expr {
        public final Object value;
        public Literal(Object value) { this.value = value; }
    }

    /** 变量引用 */
    public static class Variable implements Expr {
        public final String name;
        public Variable(String name) { this.name = name; }
    }

    /** 一元运算：-x、not x */
    public static class Unary implements Expr {
        public final qxz.lexer.TokenType op;
        public final Expr right;
        public Unary(qxz.lexer.TokenType op, Expr right) { this.op = op; this.right = right; }
    }

    /** 二元运算：算术/比较/逻辑 */
    public static class Binary implements Expr {
        public final Expr left;
        public final qxz.lexer.TokenType op;
        public final Expr right;
        public Binary(Expr left, qxz.lexer.TokenType op, Expr right) {
            this.left = left; this.op = op; this.right = right;
        }
    }

    /** 赋值：x = expr（含复合赋值 += 等，op 记录实际运算符） */
    public static class Assign implements Expr {
        public final String name;
        public final qxz.lexer.TokenType op;
        public final Expr value;
        public Assign(String name, qxz.lexer.TokenType op, Expr value) {
            this.name = name; this.op = op; this.value = value;
        }
    }

    /** 函数调用 */
    public static class Call implements Expr {
        public final Expr callee;
        public final List<Expr> arguments;
        public Call(Expr callee, List<Expr> arguments) {
            this.callee = callee; this.arguments = arguments;
        }
    }

    /** 列表字面量：[1, 2, 3] */
    public static class ListLiteral implements Expr {
        public final List<Expr> elements;
        public ListLiteral(List<Expr> elements) { this.elements = elements; }
    }

    /** 下标访问：list[i] */
    public static class Index implements Expr {
        public final Expr target;
        public final Expr index;
        public Index(Expr target, Expr index) { this.target = target; this.index = index; }
    }

    // ================= 语句 =================

    public interface Stmt { }

    /** 表达式语句 */
    public static class ExprStmt implements Stmt {
        public final Expr expr;
        public ExprStmt(Expr expr) { this.expr = expr; }
    }

    /** 变量声明：let x = expr; */
    public static class VarDecl implements Stmt {
        public final String name;
        public final Expr initializer;
        public VarDecl(String name, Expr initializer) { this.name = name; this.initializer = initializer; }
    }

    /** 代码块 */
    public static class Block implements Stmt {
        public final List<Stmt> statements;
        public Block(List<Stmt> statements) { this.statements = statements; }
    }

    /** if / elif / else */
    public static class If implements Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch; // 可为 null
        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition; this.thenBranch = thenBranch; this.elseBranch = elseBranch;
        }
    }

    /** while 循环 */
    public static class While implements Stmt {
        public final Expr condition;
        public final Stmt body;
        public While(Expr condition, Stmt body) { this.condition = condition; this.body = body; }
    }

    /** for 循环：for (init; cond; inc) body（init/inc 可为 null） */
    public static class For implements Stmt {
        public final Stmt init;
        public final Expr condition;
        public final Expr increment;
        public final Stmt body;
        public For(Stmt init, Expr condition, Expr increment, Stmt body) {
            this.init = init; this.condition = condition; this.increment = increment; this.body = body;
        }
    }

    /** break */
    public static class Break implements Stmt { }

    /** continue */
    public static class Continue implements Stmt { }

    /** 函数声明：func name(params) { body } */
    public static class FuncDecl implements Stmt {
        public final String name;
        public final List<String> params;
        public final List<Stmt> body;
        public FuncDecl(String name, List<String> params, List<Stmt> body) {
            this.name = name; this.params = params; this.body = body;
        }
    }

    /** return [expr]; */
    public static class Return implements Stmt {
        public final Expr value; // 可为 null
        public Return(Expr value) { this.value = value; }
    }

    /** section 块：结构化数据/配置描述，value 为字符串字面量 */
    public static class Section implements Stmt {
        public final String name;
        public final List<SectionEntry> entries;
        public Section(String name, List<SectionEntry> entries) { this.name = name; this.entries = entries; }
    }

    public static class SectionEntry {
        public final String key;
        public final Expr value;
        public SectionEntry(String key, Expr value) { this.key = key; this.value = value; }
    }
}
