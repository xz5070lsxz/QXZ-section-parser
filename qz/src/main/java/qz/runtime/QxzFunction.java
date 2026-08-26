package qz.runtime;

import qxz.ast.Ast;

import java.util.List;

/**
 * QXZ 用户自定义函数。由 QZ 运行时持有并调用。
 */
public class QxzFunction {
    public final String name;
    public final List<String> params;
    public final List<Ast.Stmt> body;
    public final Environment closure;

    public QxzFunction(String name, List<String> params, List<Ast.Stmt> body, Environment closure) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
    }

    public int arity() {
        return params.size();
    }

    @Override
    public String toString() {
        return "<函数 " + name + "(" + params.size() + " 参数)>";
    }
}
