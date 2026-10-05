package qxz.lexer;

/**
 * QXZ 词法单元。携带类型、字面值、所在行列，用于错误定位。
 */
public class Token {
    public final TokenType type;
    public final String lexeme;
    public final Object literal;   // 字面量的实际值（Integer/Double/String/Boolean/null）
    public final int line;
    public final int column;
    public final LineMarker marker; // 行级标记（LANG_HEADER / LANG_END 时携带），其余为 null

    public Token(TokenType type, String lexeme, Object literal, int line, int column) {
        this(type, lexeme, literal, line, column, null);
    }

    public Token(TokenType type, String lexeme, Object literal, int line, int column, LineMarker marker) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
        this.marker = marker;
    }

    @Override
    public String toString() {
        return type + " '" + lexeme + "'";
    }
}
