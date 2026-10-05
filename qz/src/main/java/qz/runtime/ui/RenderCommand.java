package qz.runtime.ui;

/**
 * QZ UI 渲染指令：宿主（安卓壳等）据此绘制一帧画面。
 * 坐标均为像素，颜色为 ARGB 整数。宿主仅需实现 UiHost 接口即可接管绘制。
 */
public class RenderCommand {

    public enum Kind { RECT, TEXT, ICON, IMAGE }

    public final Kind kind;
    public final int x, y, w, h;
    public final int color;          // RECT：填充色；TEXT：文字色
    public final int borderWidth;
    public final int borderColor;
    public final int radius;
    public final String text;
    public final int fontSize;
    public final boolean bold;
    public final int align;          // 0=左 1=中 2=右
    public final String icon;
    public final String image;

    private RenderCommand(Kind kind, int x, int y, int w, int h, int color,
                          int borderWidth, int borderColor, int radius,
                          String text, int fontSize, boolean bold, int align,
                          String icon, String image) {
        this.kind = kind;
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.color = color;
        this.borderWidth = borderWidth;
        this.borderColor = borderColor;
        this.radius = radius;
        this.text = text;
        this.fontSize = fontSize;
        this.bold = bold;
        this.align = align;
        this.icon = icon;
        this.image = image;
    }

    public static RenderCommand rect(int x, int y, int w, int h, int color,
                                     int borderWidth, int borderColor, int radius) {
        return new RenderCommand(Kind.RECT, x, y, w, h, color, borderWidth, borderColor,
                radius, null, 0, false, 0, null, null);
    }

    public static RenderCommand text(int x, int y, int w, int h, String text, int color,
                                     int fontSize, boolean bold, int align) {
        return new RenderCommand(Kind.TEXT, x, y, w, h, color, 0, 0, 0,
                text, fontSize, bold, align, null, null);
    }

    public static RenderCommand icon(int x, int y, String icon, int size, int color) {
        return new RenderCommand(Kind.ICON, x, y, size, size, color, 0, 0, 0,
                null, 0, false, 0, icon, null);
    }

    public static RenderCommand image(int x, int y, int w, int h, String image) {
        return new RenderCommand(Kind.IMAGE, x, y, w, h, 0, 0, 0, 0,
                null, 0, false, 0, null, image);
    }

    @Override
    public String toString() {
        return switch (kind) {
            case RECT -> "RECT  x=" + x + " y=" + y + " w=" + w + " h=" + h
                    + " fill=" + UiStyle.colorToHex(color)
                    + (borderWidth > 0 ? " border=" + borderWidth + " " + UiStyle.colorToHex(borderColor) : "")
                    + (radius > 0 ? " radius=" + radius : "");
            case TEXT -> "TEXT  x=" + x + " y=" + y + " w=" + w + " h=" + h
                    + " text=" + text + " color=" + UiStyle.colorToHex(color)
                    + " size=" + fontSize + (bold ? " bold" : "") + " align=" + align;
            case ICON -> "ICON  x=" + x + " y=" + y + " icon=" + icon
                    + " size=" + w + " color=" + UiStyle.colorToHex(color);
            case IMAGE -> "IMAGE x=" + x + " y=" + y + " w=" + w + " h=" + h + " image=" + image;
        };
    }
}
