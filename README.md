---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: fc9cc4777fe5c58ed1ba1eb59b80943b_1ddcea32a05911f1a54f525400f8a581
    ReservedCode1: ANUBXVFx7zZ3EOFsqn6/POoCgzNFAP4uu3hiUl3KQC88U6yAK9T3xLcEivIWbpAXLNdSO4QK/eOoX/vMZZazWG7jSm+QTC52trTN/A3uplerr4u0EcgJygyZ0uR4yDRi4+sDFnbpIbjySIRRvPf3ccQLGUVlhNVASp5FNghK+RHlmbCOtr1PfTx3EzY=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: fc9cc4777fe5c58ed1ba1eb59b80943b_1ddcea32a05911f1a54f525400f8a581
    ReservedCode2: ANUBXVFx7zZ3EOFsqn6/POoCgzNFAP4uu3hiUl3KQC88U6yAK9T3xLcEivIWbpAXLNdSO4QK/eOoX/vMZZazWG7jSm+QTC52trTN/A3uplerr4u0EcgJygyZ0uR4yDRi4+sDFnbpIbjySIRRvPf3ccQLGUVlhNVASp5FNghK+RHlmbCOtr1PfTx3EzY=
---

# QXZ-section-parser

QXZ 自研编程语言 + QZ 运行时框架。

## 项目定位

- **QXZ**：多语法前端（解析层）。语法风格基于 Java / C++ / C# 家族（类 C 语法），后续可扩展 Python / GO / PHP 风格语法。各语言解析后统一编译为 QXZ 中间表示（AST）。
- **QZ**：运行时后端（执行层）。无论什么语法写的程序，最终执行都依赖 QZ 框架。

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

- 类 Java/C++/C# 语法：`let` 声明变量、`func` 定义函数、`if/elif/else`、`while/for`、`break/continue`、`return`
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

详见 [docs/语法规范.md](docs/语法规范.md)。
*（内容由AI生成，仅供参考）*
