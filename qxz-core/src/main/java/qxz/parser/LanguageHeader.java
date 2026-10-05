package qxz.parser;

import qxz.lexer.LineMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QXZ 语言声明头（Language Header）与行级语言标记。
 *
 * 一、文件第一行可用注释形式声明程序所使用的语法与版本（v1.0 兼容）：
 *   //Java 17
 *   //Python 3
 *   //C++ 17
 *   //C# 13
 *
 * 解析约束：
 *  - 语法名与版本号之间必须有一个或多个空格（空格是分隔符，不可省略）
 *  - 语法名不能包含空格；版本号由数字和点组成
 *  - 不符合该严格模式的行视为普通注释
 *
 * 二、行级语言标记（2026-09-25 用户确认，与首行声明头并存）：
 *   //(语言类型) (版本)
 *   //(语言类型) (语言类型版本) (附加内容) (附加内容版本号)
 *   行首仅 // 时为结束标记：指定语言执行完成本行之后结束。
 * 具体解析实现见 {@link LineMarker}，本类提供全文件扫描辅助。
 */
public class LanguageHeader {

    // 严格模式：//语法名 版本号（语法名后必须有空格，版本号纯数字/点）
    private static final Pattern HEADER_PATTERN =
            Pattern.compile("^//\\s*([A-Za-z][A-Za-z0-9+#]*)\\s+(\\d+(?:\\.\\d+)*)\\s*$");

    public final String language;
    public final String version;

    private LanguageHeader(String language, String version) {
        this.language = language;
        this.version = version;
    }

    /**
     * 从源码中解析语言声明头。
     * @return 若第一行符合声明格式则返回头部信息，否则返回 null
     */
    public static LanguageHeader parse(String source) {
        if (source == null || source.isEmpty()) return null;
        String firstLine = source.split("\r?\n", 2)[0];
        Matcher m = HEADER_PATTERN.matcher(firstLine);
        if (!m.matches()) return null;
        return new LanguageHeader(m.group(1), m.group(2));
    }

    /**
     * 扫描源码中的全部行级语言标记（含结束标记），按出现顺序返回。
     * 行首声明头与行级标记格式一致，若位于首行也会被扫描到。
     *
     * @return 命中的行级标记列表；普通注释不计入
     */
    public static List<LineMarker> parseLineMarkers(String source) {
        List<LineMarker> markers = new ArrayList<>();
        if (source == null || source.isEmpty()) return markers;
        String[] lines = source.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (!trimmed.startsWith("//")) continue;
            int idx = line.indexOf("//");
            // 仅识别行首 //（// 前只有空白）的行级标记
            if (!line.substring(0, idx).trim().isEmpty()) continue;
            LineMarker marker = LineMarker.parseComment(line.substring(idx + 2), i + 1);
            if (marker != null) markers.add(marker);
        }
        return markers;
    }

    @Override
    public String toString() {
        return language + " " + version;
    }
}
