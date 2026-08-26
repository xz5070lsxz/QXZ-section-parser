package qxz.lexer;

/**
 * QXZ 语言的 Token 类型定义。
 * 语法风格基于 Java / C++ / C# 家族（类 C 语法）。
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
