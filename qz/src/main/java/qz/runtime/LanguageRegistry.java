package qz.runtime;

import qxz.lexer.LineMarker;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QZ 语言解析器注册表（Language Registry）。
 *
 * QZ 是运行时后端（主执行层）：运行 Java / C# / C++ 以及 JVM 均以 QZ 为主要执行层，
 * QXZ 语言层为次要。Java / C# / C++ 三者语法书写一致（同属 QXZ 统一语法体系），
 * 对应代码行前端通过行级语言标记（LineMarker）声明语言类型与版本。
 *
 * 内置注册：Java / C++ / C#（版本不限，统一语法体系）。
 * 其他风格语法（如 Python / GO / PHP）需要 QZ 扩展模块注册对应解析器。
 */
public class LanguageRegistry {

    /** 已注册语言（小写）→ 解析器描述 */
    private final Map<String, String> parsers = new LinkedHashMap<>();

    public LanguageRegistry() {
        // Java / C# / C++ 语法书写一致，同属 QXZ 统一语法体系，由 QZ 统一执行
        register("java", "内置：QXZ 统一语法体系（Java 风格）");
        register("c++", "内置：QXZ 统一语法体系（Java/C#/C++ 语法一致）");
        register("c#", "内置：QXZ 统一语法体系（Java/C#/C++ 语法一致）");
    }

    /** 注册语言解析器（扩展点：QZ 扩展模块接入 Python / GO / PHP 等）。 */
    public void register(String language, String description) {
        parsers.put(language.toLowerCase(), description);
    }

    public boolean isRegistered(String language) {
        return language != null && parsers.containsKey(language.toLowerCase());
    }

    /**
     * 激活行级语言标记：校验语言是否已注册，并处理附加内容
     * （如 JVM 附加内容由 QZ 作为宿主装载，而非依赖系统级 JVM）。
     *
     * @param marker  行级语言标记
     * @param jvmHost QZ JVM 宿主
     */
    public void activate(LineMarker marker, JvmHost jvmHost) {
        String lang = marker.language == null ? "" : marker.language.toLowerCase();
        if (!isRegistered(lang)) {
            throw new QzRuntimeException("QZ 未注册语法解析器: " + marker.language + " " + marker.version
                    + "（已注册: " + String.join(", ", parsers.keySet()) + "）", marker.line);
        }
        if (marker.hasAttachment()) {
            String att = marker.attachment.toLowerCase();
            if ("jvm".equals(att)) {
                jvmHost.attach(marker.attachmentVersion, marker.line);
            } else {
                throw new QzRuntimeException("未知附加内容: " + marker.attachment, marker.line);
            }
        }
    }
}
