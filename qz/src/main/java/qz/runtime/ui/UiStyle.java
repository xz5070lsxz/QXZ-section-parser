package qz.runtime.ui;

import java.util.Map;

/**
 * QZ UI 样式与布局属性解析。
 * 布局模型为流式盒模型（简化 flexbox）：row / column 两种主轴，
 * width/height 支持固定像素、"fill"（占满剩余）或 "wrap"（自适应内容），
 * 对齐支持 start / center / end / stretch（交叉轴）。
 * 颜色支持各种值：#RGB、#RRGGBB、#RRGGBBAA、命名色（black/white/red/green/blue 等）。
 */
public class UiStyle {

    /** 尺寸标记 */
    public static final int SIZE_FILL = -1;
    public static final int SIZE_WRAP = -2;

    /** 对齐枚举值 */
    public static final int ALIGN_STRETCH = 0;
    public static final int ALIGN_START = 1;
    public static final int ALIGN_CENTER = 2;
    public static final int ALIGN_END = 3;

    // ---- 布局 ----
    public int width = SIZE_WRAP;
    public int height = SIZE_WRAP;
    public String direction = "column";   // row | column
    public int gap = 0;
    public int padding = 0;
    public int margin = 0;
    public int align = ALIGN_STRETCH;     // 交叉轴对齐
    public int justify = ALIGN_START;     // 主轴对齐

    // ---- 外观 ----
    public int bg = 0x00000000;           // 背景色 ARGB
    public int border = 0;                // 边框宽度（0 = 无边框）
    public int borderColor = 0xFF000000;  // 边框颜色
    public int radius = 0;                // 圆角半径

    // ---- 内容 ----
    public String text = null;            // 文本内容（label/value/text）
    public int fontSize = 16;
    public int color = 0xFF000000;        // 文字/图标颜色
    public boolean bold = false;
    public String icon = null;            // 图标名（宿主资源表）
    public int iconSize = 24;
    public String image = null;           // 图片资源名（宿主资源表）

    public UiStyle(Map<String, Object> props) {
        if (props == null) return;
        for (Map.Entry<String, Object> e : props.entrySet()) {
            apply(e.getKey(), e.getValue());
        }
    }

    private void apply(String key, Object v) {
        switch (key) {
            case "width" -> width = parseSize(v);
            case "height" -> height = parseSize(v);
            case "direction" -> direction = str(v);
            case "gap" -> gap = asInt(v);
            case "padding" -> padding = asInt(v);
            case "margin" -> margin = asInt(v);
            case "align" -> align = parseAlign(v);
            case "justify" -> justify = parseAlign(v);
            case "bg", "background" -> bg = parseColor(v);
            case "border" -> border = asInt(v);
            case "border_color" -> borderColor = parseColor(v);
            case "radius" -> radius = asInt(v);
            case "font_size" -> fontSize = asInt(v);
            case "color" -> color = parseColor(v);
            case "bold" -> bold = asBool(v);
            case "label", "value", "text" -> text = str(v);
            case "icon" -> icon = str(v);
            case "icon_size" -> iconSize = asInt(v);
            case "image" -> image = str(v);
            default -> { /* 未知属性保留扩展空间，不报错 */ }
        }
    }

    public static int parseSize(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        String s = str(v);
        if ("fill".equals(s)) return SIZE_FILL;
        if ("wrap".equals(s)) return SIZE_WRAP;
        throw new qz.runtime.QzRuntimeException("尺寸只支持数字、\"fill\" 或 \"wrap\"，实际为: " + s);
    }

    public static int parseAlign(Object v) {
        String s = str(v);
        return switch (s) {
            case "stretch" -> ALIGN_STRETCH;
            case "start" -> ALIGN_START;
            case "center" -> ALIGN_CENTER;
            case "end" -> ALIGN_END;
            default -> throw new qz.runtime.QzRuntimeException("对齐只支持 stretch/start/center/end，实际为: " + s);
        };
    }

    /** 解析各种颜色值：数字 0xRRGGBB / 0xAARRGGBB，字符串 #RGB/#RRGGBB/#RRGGBBAA/命名色 */
    public static int parseColor(Object v) {
        if (v instanceof Number) {
            int c = ((Number) v).intValue() & 0xFFFFFFFF;
            // 数值若无 alpha 位（<= 0xFFFFFF）视为不透明 RGB
            return (c & 0xFF000000) == 0 ? (c | 0xFF000000) : c;
        }
        String s = str(v).trim();
        if (s.startsWith("#")) {
            String hex = s.substring(1);
            long val;
            try {
                val = Long.parseLong(hex, 16);
            } catch (NumberFormatException e) {
                throw new qz.runtime.QzRuntimeException("无效颜色: " + s);
            }
            return switch (hex.length()) {
                case 3 -> {
                    int r = (int) ((val >> 8) & 0xF);
                    int g = (int) ((val >> 4) & 0xF);
                    int b = (int) (val & 0xF);
                    yield 0xFF000000 | (r << 20) | (r << 16) | (g << 12) | (g << 8) | (b << 4) | b;
                }
                case 6 -> (int) (0xFF000000L | val);
                case 8 -> (int) val;
                default -> throw new qz.runtime.QzRuntimeException("无效颜色: " + s);
            };
        }
        Integer named = NAMED_COLORS.get(s.toLowerCase());
        if (named != null) return named;
        throw new qz.runtime.QzRuntimeException("未知颜色: " + s);
    }

    private static final Map<String, Integer> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", 0xFF000000),
            Map.entry("white", 0xFFFFFFFF),
            Map.entry("red", 0xFFFF0000),
            Map.entry("green", 0xFF00FF00),
            Map.entry("blue", 0xFF0000FF),
            Map.entry("yellow", 0xFFFFFF00),
            Map.entry("orange", 0xFFFFA500),
            Map.entry("purple", 0xFF800080),
            Map.entry("cyan", 0xFF00FFFF),
            Map.entry("magenta", 0xFFFF00FF),
            Map.entry("gray", 0xFF808080),
            Map.entry("grey", 0xFF808080),
            Map.entry("lightgray", 0xFFD3D3D3),
            Map.entry("darkgray", 0xFF404040),
            Map.entry("transparent", 0x00000000)
    );

    private static String str(Object v) { return String.valueOf(v); }
    private static int asInt(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(String.valueOf(v));
    }
    private static boolean asBool(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    /** ARGB 颜色转 CSS 风格字符串，便于调试输出 */
    public static String colorToHex(int c) {
        int a = (c >>> 24) & 0xFF, r = (c >>> 16) & 0xFF, g = (c >>> 8) & 0xFF, b = c & 0xFF;
        if (a == 0xFF) return String.format("#%02X%02X%02X", r, g, b);
        return String.format("#%02X%02X%02X%02X", r, g, b, a);
    }
}
