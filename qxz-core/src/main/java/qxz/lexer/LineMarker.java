package qxz.lexer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 行级语言标记（Line-Level Language Marker）。
 *
 * 运行 Java / C# / C++ 时，三者语法书写一致（同属 QXZ 统一语法体系），
 * 代码行前端必须添加语言类型与版本标记：
 *   //Java 25
 *   //C++ 20
 *   //C# 12
 *
 * 需要附加内容（如 JVM）时，在对应行前端增加：
 *   //(语言类型) (语言类型版本) (附加内容) (附加内容版本号)
 *   //Java 25 JVM 25
 *
 * 结束标记：行首仅 //（无语言类型 / 版本 / 附加内容）时，作为结束标记，
 * 代表指定语言执行完成本行之后结束（区别于普通注释）。
 *
 * 解析约束（严格模式）：
 *  - 语言类型 / 附加内容不能包含空格（允许字母、数字、+、#）
 *  - 版本号由数字和点组成
 *  - 各字段之间必须有一个或多个空格（空格是分隔符，不可省略）
 *  - 不符合严格模式的行视为普通注释（仅 // 为显式结束标记）
 */
public class LineMarker {

    /** 严格模式行级标记：//语言 版本 [附加内容 附加内容版本] */
    private static final Pattern MARKER_PATTERN = Pattern.compile(
            "^//\\s*([A-Za-z][A-Za-z0-9+#]*)\\s+(\\d+(?:\\.\\d+)*)"
                    + "(?:\\s+([A-Za-z][A-Za-z0-9+#]*)\\s+(\\d+(?:\\.\\d+)*))?\\s*$");

    public final int line;
    public final String language;
    public final String version;
    public final String attachment;        // 附加内容（如 JVM），可为 null
    public final String attachmentVersion; // 附加内容版本号，可为 null
    public final boolean end;              // 是否为结束标记（行首仅 //）

    private LineMarker(int line, String language, String version,
                       String attachment, String attachmentVersion, boolean end) {
        this.line = line;
        this.language = language;
        this.version = version;
        this.attachment = attachment;
        this.attachmentVersion = attachmentVersion;
        this.end = end;
    }

    /**
     * 从一整行注释文本解析行级标记。
     *
     * @param commentText 不含 // 前缀、不含行尾换行的注释文本
     * @param line        所在行号（从 1 开始）
     * @return 命中严格模式返回标记对象；行首仅 // 返回结束标记；其余返回 null（普通注释）
     */
    public static LineMarker parseComment(String commentText, int line) {
        if (commentText == null) return null;
        String trimmed = commentText.trim();
        if (trimmed.isEmpty()) {
            // 行首仅 //（无语言类型 / 版本 / 附加内容）→ 结束标记
            return new LineMarker(line, null, null, null, null, true);
        }
        Matcher m = MARKER_PATTERN.matcher("//" + commentText);
        if (!m.matches()) return null;
        return new LineMarker(line, m.group(1), m.group(2), m.group(3), m.group(4), false);
    }

    public boolean hasAttachment() {
        return attachment != null && !attachment.isEmpty();
    }

    public boolean isEndMarker() {
        return end;
    }

    @Override
    public String toString() {
        if (end) return "行 " + line + ": 结束标记 //";
        if (hasAttachment()) {
            return "行 " + line + ": " + language + " " + version
                    + " " + attachment + " " + attachmentVersion;
        }
        return "行 " + line + ": " + language + " " + version;
    }
}
