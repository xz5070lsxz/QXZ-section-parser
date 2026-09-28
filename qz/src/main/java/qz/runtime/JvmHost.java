package qz.runtime;

/**
 * QZ JVM 宿主（JVM Host）。
 *
 * 定位：鸿蒙 HarmonyOS 5+ 通常不允许运行系统级 JVM，因此由 QZ 运行时框架
 * 直接提供 JVM 装载能力，作为运行 Java / JVM 应用的宿主方案。
 * QXZ 语言层为次要执行层；运行 Java / C# / C++ 以及 JVM 均以 QZ 为主。
 *
 * 使用方式：代码行前端声明行级标记附加内容 —— //Java 25 JVM 25，
 * 由 LanguageRegistry 校验后调用本宿主装载对应版本 JVM。
 * 当前版本为框架级宿主抽象；装载真实 JVM 由 QZ 扩展模块实现。
 */
public class JvmHost {

    private boolean attached;
    private String attachedVersion;

    /** 装载指定版本 JVM；同版本幂等，不同版本在同一运行段内拒绝切换。 */
    public void attach(String version, int line) {
        if (version == null || version.isEmpty()) {
            throw new QzRuntimeException("JVM 附加内容必须携带版本号", line);
        }
        if (attached && !version.equals(attachedVersion)) {
            throw new QzRuntimeException("QZ 已装载 JVM " + attachedVersion
                    + "，同一运行段内不能切换为 " + version, line);
        }
        attached = true;
        attachedVersion = version;
    }

    public boolean isAttached() {
        return attached;
    }

    public String getAttachedVersion() {
        return attachedVersion;
    }

    @Override
    public String toString() {
        return attached ? "QZ JVM 宿主已装载 JVM " + attachedVersion
                        : "QZ JVM 宿主未装载（等待行级标记附加内容）";
    }
}
