# QXZ-section-parser

QXZ 纯自主研发编程语言 + QZ 运行时框架。

## 项目定位

- **QXZ**：**纯自主研发**的编程语言。语法体系、中间表示（AST）与运行时均由 QXZ 自主设计实现，不基于、不继承任何既有语言。作为多语法前端，可解析 Java / Python / GO / PHP 等风格语法（解析能力，非语言依赖）；各语言解析后统一编译为 QXZ 中间表示（AST）。
- **QZ**：运行时后端（执行层）。无论什么语法写的程序，最终执行都依赖 QZ 框架。✅能力方向（用户确认，2026-09-25）：**运行 Java / C# / C++ 以及 JVM 均以 QZ 为主要执行层，QXZ 语言层为次要**——QZ 具备运行 JVM 的能力，作为鸿蒙 HarmonyOS 5+（通常不允许运行系统级 JVM）上运行 Minecraft Java 等 JVM 应用的宿主方案。
- **文件后缀**：QXZ 源码文件后缀**通常为 `.qzm`**（用户确认，2026-09-23）。
- **行级语言标记（用户确认，2026-09-25）**：运行 **Java / C# / C++** 时三者语法书写一致；对应行前端必须以注释标记语言类型与版本——`//(语言类型) (版本)`；需附加内容（如 JVM）时用 `//(语言类型) (语言类型版本) (附加内容) (附加内容版本号)`；行首**仅 `//`** 时为**结束标记**，代表指定语言执行完成本行之后结束。

即：**QXZ 负责"看得懂"，QZ 负责"跑得起来"**。

## 目录结构

```
QXZ-section-parser/
├── qxz-core/          # QXZ 语言核心（前端）
│   └── src/main/java/qxz/
│       ├── lexer/     # 词法分析器
│       ├── parser/    # 语法解析器
│       ├── ast/       # 语法树模型
│       └── Main.java  # 命令行入口
├── qz/                # QZ 运行时框架（后端）
│   └── src/main/java/qz/runtime/
│       ├── Interpreter.java      # 解释器/执行引擎
│       ├── Environment.java      # 变量作用域
│       ├── QxzFunction.java      # 函数对象
│       └── QzRuntimeException.java
├── examples/          # 示例程序（.qxz）
├── docs/              # 语言规范文档
├── build.sh           # 构建脚本
└── README.md
```

## 快速开始

依赖：JDK 17+（或使用项目内置的免安装 JDK）。

```bash
# 构建
./build.sh

# 运行 QXZ 程序
java -cp build qxz.Main run examples/hello.qxz

# 其他命令
java -cp build qxz.Main lex <文件.qxz>   # 词法分析
java -cp build qxz.Main ast <文件.qxz>   # 语法树
java -cp build qxz.Main repl             # 交互式命令行
```

## 特性

- 自主设计的语法体系：`let` 声明变量、`func` 定义函数、`if/elif/else`、`while/for`、`break/continue`、`return`
- **语言声明头**：第一行 `//语法名 版本号` 声明运行语法（语法名与版本号间必须有空格），未注册语法明确报错
- 动态类型：int / double / string / boolean / null / list / 配置块
- 复合赋值与自增自减：`+=` `-=` `*=` `/=` `++` `--`
- 注释：`//` 行注释、`/* */` 块注释、`#` 行注释
- **section 块**（QXZ 特色）：结构化配置/数据描述，无需引号噪音
- 标准库：print / input / len / range / type / toInt / toDouble / toString / abs / max / min / sqrt / floor / ceil

## 示例

```java
// 变量与函数
let name = "QXZ";
func add(a, b) {
    return a + b;
}
print(add(3, 4));        // 输出 7

// 循环
let sum = 0;
for (let i = 1; i <= 100; i++) {
    sum += i;
}
print(sum);              // 输出 5050

// section 配置块（QXZ 特色）
section app_config {
    name: "demo";
    version: 1.0;
    enabled: true;
}
print(app_config["name"]);
```

详见 [docs/QXZ语法规范-V1.1.md](docs/QXZ语法规范-V1.1.md)。
