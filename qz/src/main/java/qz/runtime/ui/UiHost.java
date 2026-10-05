package qz.runtime.ui;

import java.util.List;

/**
 * QZ UI 宿主桥接口。
 * QZ 运行时完成 ui 块解析、布局计算后，将渲染指令列表交给宿主绘制。
 * 默认实现为控制台调试输出（DefaultUiHost）；安卓壳等平台可自行实现接管绘制。
 */
public interface UiHost {

    /**
     * 展示一帧 UI。
     *
     * @param screenName 界面名称（ui 块名）
     * @param width      画布宽（像素）
     * @param height     画布高（像素）
     * @param commands   渲染指令列表（已按绘制顺序排列）
     */
    void show(String screenName, int width, int height, List<RenderCommand> commands);
}
