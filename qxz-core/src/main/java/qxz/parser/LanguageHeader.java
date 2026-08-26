package qxz.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QXZ 语言声明头（Language Header）。
 *
 * 规则：文件第一行可用注释形式声明程序所使用的语法与版本：
 *   //Java 17
 *   //Python 3
 *   //C++ 17
 *   //C# 13
 *
 * 解析约束：
 *  - 语法名与版本号之间必须有一个或多个空格（空格是分隔符，不可省略）
 *  - 语法名不能包含空格；版本号由数字和点组成
 *  - 不符合该严格模式的行视为普通注释
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

    @Override
    public String toString() {
        return language + " " + version;
    }
}
