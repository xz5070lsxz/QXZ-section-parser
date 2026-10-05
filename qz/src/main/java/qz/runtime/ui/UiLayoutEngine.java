package qz.runtime.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * QZ UI 布局引擎（流式盒模型，简化 flexbox）。
 *
 * 依据 EXCOYZNVM 设计稿归纳的多区布局（顶部栏 + 左菜单 + 中间内容区 + 右功能栏）设计：
 * - direction: row（横向排列）/ column（纵向排列）
 * - width / height: 固定像素、"fill"（占满主轴剩余）或 "wrap"（按内容自适应）
 * - gap: 子节点主轴间距；padding: 内边距；margin: 外边距
 * - align: 交叉轴对齐（stretch 拉伸 / start / center / end）
 * - justify: 主轴对齐（start / center / end）
 * - 颜色: 支持各种颜色值（UiStyle.parseColor）
 *
 * 布局完成后输出 RenderCommand 指令列表，交由 UiHost 渲染。
 */
public class UiLayoutEngine {

    /** 默认画布尺寸：对齐设计稿主流规格（1094x547 横屏） */
    public static final int DEFAULT_CANVAS_WIDTH = 1094;
    public static final int DEFAULT_CANVAS_HEIGHT = 547;

    /** 布局结果：指令列表 + 画布尺寸 */
    public static class LayoutResult {
        public final int width;
        public final int height;
        public final List<RenderCommand> commands;

        LayoutResult(int width, int height, List<RenderCommand> commands) {
            this.width = width;
            this.height = height;
            this.commands = commands;
        }
    }

    /** 对 ui 块根节点执行布局，返回渲染指令与画布尺寸 */
    public LayoutResult layout(UiNode root) {
        UiStyle s = new UiStyle(root.props);
        int w = s.width >= 0 ? s.width : DEFAULT_CANVAS_WIDTH;
        int h = s.height >= 0 ? s.height : DEFAULT_CANVAS_HEIGHT;
        List<RenderCommand> cmds = new ArrayList<>();
        place(root, 0, 0, w, h, cmds);
        return new LayoutResult(w, h, cmds);
    }

    // ============ 递归布局 ============

    private void place(UiNode node, int x, int y, int w, int h, List<RenderCommand> cmds) {
        UiStyle s = new UiStyle(node.props);

        // 1. 绘制节点自身
        emitNode(node, s, x, y, w, h, cmds);
        if (node.children == null || node.children.isEmpty()) return;

        // 2. 内容区（扣除 padding）
        int cx = x + s.padding;
        int cy = y + s.padding;
        int cw = Math.max(0, w - s.padding * 2);
        int ch = Math.max(0, h - s.padding * 2);

        // 3. 主轴/交叉轴方向
        boolean row = "row".equals(s.direction);
        int gap = s.gap;

        // 4. 统计子节点主轴尺寸：固定值累加、fill 计数
        int totalFixed = 0;
        int fillCount = 0;
        for (UiNode kid : node.children) {
            UiStyle ks = new UiStyle(kid.props);
            if (row) {
                if (ks.width >= 0) totalFixed += ks.width;
                else fillCount++;
            } else {
                if (ks.height >= 0) totalFixed += ks.height;
                else fillCount++;
            }
        }
        int availMain = row ? cw : ch;
        int gapTotal = gap * Math.max(0, node.children.size() - 1);
        int remain = Math.max(0, availMain - totalFixed - gapTotal);
        int fillMain = fillCount > 0 ? remain / fillCount : 0;

        // 5. 主轴游标（含 justify 偏移）
        int used = totalFixed + fillCount * fillMain + gapTotal;
        int cursor = (row ? cx : cy);
        if (s.justify == UiStyle.ALIGN_CENTER) cursor = (row ? cx : cy) + Math.max(0, (availMain - used) / 2);
        else if (s.justify == UiStyle.ALIGN_END) cursor = (row ? cx : cy) + Math.max(0, availMain - used);

        // 6. 逐个摆放子节点
        for (UiNode kid : node.children) {
            UiStyle ks = new UiStyle(kid.props);

            if (row) {
                int kw = ks.width >= 0 ? ks.width : (ks.width == UiStyle.SIZE_FILL ? fillMain : wrapWidth(kid, ks, cw));
                int kh;
                if (ks.height >= 0) kh = ks.height;
                else if (ks.align == UiStyle.ALIGN_STRETCH) kh = ch;
                else kh = wrapHeight(kid, ks, cw);
                int ky = cy;
                if (ks.align == UiStyle.ALIGN_CENTER) ky = cy + Math.max(0, (ch - kh) / 2);
                else if (ks.align == UiStyle.ALIGN_END) ky = cy + Math.max(0, ch - kh);
                place(kid, cursor, ky, Math.max(0, kw), Math.max(0, kh), cmds);
                cursor += kw + gap;
            } else {
                int kh = ks.height >= 0 ? ks.height : (ks.height == UiStyle.SIZE_FILL ? fillMain : wrapHeight(kid, ks, cw));
                int kw;
                if (ks.width >= 0) kw = ks.width;
                else if (ks.align == UiStyle.ALIGN_STRETCH) kw = cw;
                else kw = wrapWidth(kid, ks, ch);
                int kx = cx;
                if (ks.align == UiStyle.ALIGN_CENTER) kx = cx + Math.max(0, (cw - kw) / 2);
                else if (ks.align == UiStyle.ALIGN_END) kx = cx + Math.max(0, cw - kw);
                place(kid, kx, cursor, Math.max(0, kw), Math.max(0, kh), cmds);
                cursor += kh + gap;
            }
        }
    }

    // ============ 节点自身渲染 ============

    private void emitNode(UiNode node, UiStyle s, int x, int y, int w, int h, List<RenderCommand> cmds) {
        switch (node.type) {
            case "text" -> {
                if (s.text != null) {
                    cmds.add(RenderCommand.text(x, y, w, h, s.text, s.color, s.fontSize, s.bold,
                            s.align == UiStyle.ALIGN_CENTER ? 1 : (s.align == UiStyle.ALIGN_END ? 2 : 0)));
                }
                if (s.bg != 0) cmds.add(RenderCommand.rect(x, y, w, h, s.bg, 0, 0, s.radius));
            }
            case "icon" -> {
                if (s.icon != null) cmds.add(RenderCommand.icon(x, y, s.icon, s.iconSize, s.color));
                if (s.bg != 0) cmds.add(RenderCommand.rect(x, y, w, h, s.bg, 0, 0, s.radius));
            }
            case "image" -> {
                if (s.image != null) cmds.add(RenderCommand.image(x, y, w, h, s.image));
                if (s.bg != 0) cmds.add(RenderCommand.rect(x, y, w, h, s.bg, 0, 0, s.radius));
            }
            case "divider" -> {
                // 分割线：细矩形
                cmds.add(RenderCommand.rect(x, y, w, Math.max(1, h), s.borderColor, 0, 0, 0));
            }
            default -> {
                // panel / button / input / 未知类型：矩形 + 边框 + 文本
                if (s.bg != 0 || s.border > 0) {
                    cmds.add(RenderCommand.rect(x, y, w, h, s.bg, s.border, s.borderColor, s.radius));
                }
                if (s.text != null) {
                    // 文本内容：button 居中，其他默认左对齐
                    int align = "button".equals(node.type) ? 1
                            : (s.align == UiStyle.ALIGN_CENTER ? 1 : (s.align == UiStyle.ALIGN_END ? 2 : 0));
                    int tx = x;
                    int ty = y + Math.max(0, (h - (int) (s.fontSize * 1.4)) / 2);
                    if (align == 1) tx = x + Math.max(0, (w - measureTextWidth(s.text, s.fontSize)) / 2);
                    else if (align == 2) tx = x + Math.max(0, w - measureTextWidth(s.text, s.fontSize));
                    cmds.add(RenderCommand.text(tx, ty, w, h, s.text, s.color, s.fontSize, s.bold, align));
                }
                if (s.icon != null) {
                    int ix = x + Math.max(0, (w - s.iconSize) / 2);
                    int iy = y + Math.max(0, (h - s.iconSize) / 2);
                    cmds.add(RenderCommand.icon(ix, iy, s.icon, s.iconSize, s.color));
                }
            }
        }
    }

    // ============ wrap 尺寸估计 ============

    private int wrapWidth(UiNode node, UiStyle s, int crossAvail) {
        int inner = 0;
        if (node.children != null) {
            for (UiNode kid : node.children) {
                UiStyle ks = new UiStyle(kid.props);
                int kidW;
                if (ks.width >= 0) kidW = ks.width;
                else if (ks.width == UiStyle.SIZE_FILL) kidW = 0; // fill 不参与 wrap 累加
                else kidW = wrapWidth(kid, ks, crossAvail);
                inner = Math.max(inner, kidW);
            }
        }
        int content = switch (node.type) {
            case "text" -> s.text != null ? measureTextWidth(s.text, s.fontSize) : 0;
            case "icon" -> s.iconSize;
            case "image" -> 0;
            case "divider" -> 0;
            default -> {
                int t = s.text != null ? measureTextWidth(s.text, s.fontSize) : 0;
                yield inner > 0 ? inner : t;
            }
        };
        return content + s.padding * 2;
    }

    private int wrapHeight(UiNode node, UiStyle s, int crossAvail) {
        int inner = 0;
        if (node.children != null && !"row".equals(s.direction)) {
            for (UiNode kid : node.children) {
                UiStyle ks = new UiStyle(kid.props);
                int kidH;
                if (ks.height >= 0) kidH = ks.height;
                else if (ks.height == UiStyle.SIZE_FILL) kidH = 0;
                else kidH = wrapHeight(kid, ks, crossAvail);
                inner += kidH;
            }
            inner += s.gap * Math.max(0, node.children.size() - 1);
        }
        int content = switch (node.type) {
            case "text" -> (int) (s.fontSize * 1.4);
            case "icon" -> s.iconSize;
            case "image" -> 0;
            case "divider" -> 1;
            default -> inner > 0 ? inner : (s.text != null ? (int) (s.fontSize * 1.4) + 8 : 0);
        };
        return content + s.padding * 2;
    }

    /** 文本宽度估算：中文全宽 fontSize，ASCII 半宽 0.55*fontSize */
    static int measureTextWidth(String text, int fontSize) {
        double w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 128) w += fontSize * 0.55;
            else w += fontSize;
        }
        return (int) Math.ceil(w);
    }
}
