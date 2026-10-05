package qz.runtime.ui;

import java.util.List;
import java.util.Map;

/**
 * QZ UI 运行时节点：由 QXZ 的 ui 块 AST 转换而来，props 已求值为运行值。
 * 类型：panel（容器）/ button / text / input / image / icon / divider（分割线）等。
 */
public class UiNode {
    public final String type;
    public final String name;
    public final Map<String, Object> props;
    public final List<UiNode> children;

    public UiNode(String type, String name, Map<String, Object> props, List<UiNode> children) {
        this.type = type;
        this.name = name;
        this.props = props;
        this.children = children;
    }

    public Object prop(String key) {
        return props == null ? null : props.get(key);
    }
}
