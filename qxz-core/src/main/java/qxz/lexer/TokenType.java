package qxz.lexer;

/**
 * QXZ 语言的 Token 类型定义。
 * 语法体系、中间表示（AST）与运行时均由 QXZ 自主设计实现，不基于、不继承任何既有语言；
 * 可兼容解析类 C 风格书写习惯（解析能力，非语言依赖）。
 */
public enum TokenType {
    // 字面量
    IDENTIFIER, INT, DOUBLE, STRING, TRUE, FALSE, NULL,

    // 关键字
    LET,       // let 声明变量
    FUNC,      // func 声明函数
    IF, ELIF, ELSE,
    WHILE, FOR,
    RETURN, BREAK, CONTINUE,
    AND, OR, NOT,
    UI,        // ui 声明 UI 界面（QZ UI 引擎）

    // 行级语言标记
    LANG_HEADER, // 行首 //语言 版本 [附加内容 附加内容版本]（严格模式，如 //Java 25 JVM 25）
    LANG_END,    // 行首仅 //：结束标记，指定语言执行完成本行之后结束

    // 运算符
    PLUS, MINUS, STAR, SLASH, PERCENT,
    EQ, NEQ, LT, LTE, GT, GTE,
    ASSIGN,        // =
    PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN,  // += -= *= /=
    INC, DEC,      // ++ --

    // 分隔符
    LPAREN, RPAREN,     // ( )
    LBRACE, RBRACE,     // { }
    LBRACKET, RBRACKET, // [ ]
    COMMA, SEMICOLON, DOT, COLON,

    EOF
}
