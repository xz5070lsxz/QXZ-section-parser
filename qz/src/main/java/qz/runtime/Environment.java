package qz.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * QZ 运行时作用域环境。
 * 支持变量定义/读取/赋值、以及父作用域链（块级/函数级作用域）。
 */
public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    private final Environment parent;

    public Environment() {
        this(null);
    }

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    public Object get(String name) {
        if (values.containsKey(name)) return values.get(name);
        if (parent != null) return parent.get(name);
        throw new QzRuntimeException("未定义的变量 '" + name + "'");
    }

    public boolean contains(String name) {
        return values.containsKey(name) || (parent != null && parent.contains(name));
    }

    public void assign(String name, Object value) {
        if (values.containsKey(name)) {
            values.put(name, value);
            return;
        }
        if (parent != null) {
            parent.assign(name, value);
            return;
        }
        throw new QzRuntimeException("未定义的变量 '" + name + "'");
    }

    public Environment child() {
        return new Environment(this);
    }
}
