package qz.runtime.ui;

import java.util.List;

/**
 * 默认 UI 宿主：控制台调试输出。
 * 将布局结果与渲染指令打印到标准输出，供命令行构建验证；
 * 安卓壳适配可另建 UiHost 实现替换（setUiHost）。
 */
public class DefaultUiHost implements UiHost {

    @Override
    public void show(String screenName, int width, int height, List<RenderCommand> commands) {
        System.out.println("[UI] " + screenName + " " + width + "x" + height
                + " 指令数=" + commands.size());
        for (RenderCommand cmd : commands) {
            System.out.println("  " + cmd);
        }
    }
}
