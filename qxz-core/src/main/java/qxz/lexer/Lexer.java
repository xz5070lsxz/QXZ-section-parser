package qxz.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QXZ 词法分析器。
 * 将源代码字符串扫描为 Token 流，支持：
 *  - 数字（整数/浮点）、字符串（单双引号）、标识符
 *  - 类 C 运算符（含 += -= *= /= ++ --）
 *  - 注释：// 行注释、/* 块注释 *​/、# 行注释（兼容风格）
 *  - 关键字：let func if elif else while for return break continue and or not true false null
 */
public class Lexer {
    private final String src;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0, current = 0, line = 1, column = 1;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("let", TokenType.LET);
        KEYWORDS.put("func", TokenType.FUNC);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("elif", TokenType.ELIF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("break", TokenType.BREAK);
        KEYWORDS.put("continue", TokenType.CONTINUE);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("null", TokenType.NULL);
    }

    public Lexer(String source) {
        this.src = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        addToken(TokenType.EOF, null);
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LPAREN, null); break;
            case ')': addToken(TokenType.RPAREN, null); break;
            case '{': addToken(TokenType.LBRACE, null); break;
            case '}': addToken(TokenType.RBRACE, null); break;
            case '[': addToken(TokenType.LBRACKET, null); break;
            case ']': addToken(TokenType.RBRACKET, null); break;
            case ',': addToken(TokenType.COMMA, null); break;
            case ';': addToken(TokenType.SEMICOLON, null); break;
            case '.': addToken(TokenType.DOT, null); break;
            case ':': addToken(TokenType.COLON, null); break;

            case '+':
                if (match('+')) addToken(TokenType.INC, null);
                else if (match('=')) addToken(TokenType.PLUS_ASSIGN, null);
                else addToken(TokenType.PLUS, null);
                break;
            case '-':
                if (match('-')) addToken(TokenType.DEC, null);
                else if (match('=')) addToken(TokenType.MINUS_ASSIGN, null);
                else addToken(TokenType.MINUS, null);
                break;
            case '*':
                addToken(match('=') ? TokenType.STAR_ASSIGN : TokenType.STAR, null);
                break;
            case '/':
                if (match('/')) { while (peek() != '\n' && !isAtEnd()) advance(); }
                else if (match('*')) { blockComment(); }
                else addToken(match('=') ? TokenType.SLASH_ASSIGN : TokenType.SLASH, null);
                break;
            case '%': addToken(TokenType.PERCENT, null); break;

            case '=':
                addToken(match('=') ? TokenType.EQ : TokenType.ASSIGN, null);
                break;
            case '!':
                addToken(match('=') ? TokenType.NEQ : TokenType.NOT, null);
                break;
            case '<':
                addToken(match('=') ? TokenType.LTE : TokenType.LT, null);
                break;
            case '>':
                addToken(match('=') ? TokenType.GTE : TokenType.GT, null);
                break;

            case '&':
                if (match('&')) addToken(TokenType.AND, null);
                else throw error("意外的字符 '&'，QXZ 使用 and 表示逻辑与");
                break;
            case '|':
                if (match('|')) addToken(TokenType.OR, null);
                else throw error("意外的字符 '|'，QXZ 使用 or 表示逻辑或");
                break;

            case '"': case '\'': string(c); break;
            case '#': while (peek() != '\n' && !isAtEnd()) advance(); break;

            case ' ': case '\r': case '\t': break;
            case '\n': line++; column = 1; break;

            default:
                if (isDigit(c)) number();
                else if (isAlpha(c)) identifier();
                else throw error("意外的字符 '" + c + "'");
        }
    }

    private void blockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance(); advance();
                return;
            }
            if (peek() == '\n') { line++; column = 1; }
            advance();
        }
        throw error("未闭合的块注释");
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = src.substring(start, current);
        TokenType type = KEYWORDS.get(text);
        addToken(type != null ? type : TokenType.IDENTIFIER, type == null ? text : null);
    }

    private void number() {
        while (isDigit(peek())) advance();
        boolean isDouble = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isDouble = true;
            advance();
            while (isDigit(peek())) advance();
        }
        String text = src.substring(start, current);
        if (isDouble) addToken(TokenType.DOUBLE, Double.parseDouble(text));
        else addToken(TokenType.INT, Integer.parseInt(text));
    }

    private void string(char quote) {
        StringBuilder sb = new StringBuilder();
        while (peek() != quote && !isAtEnd()) {
            char c = advance();
            if (c == '\\') {
                char esc = advance();
                switch (esc) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    case '\'': sb.append('\''); break;
                    case '0': sb.append('\0'); break;
                    default: sb.append(esc);
                }
            } else {
                sb.append(c);
                if (c == '\n') line++;
            }
        }
        if (isAtEnd()) throw error("未闭合的字符串");
        advance(); // 闭合引号
        addToken(TokenType.STRING, sb.toString());
    }

    private boolean match(char expected) {
        if (isAtEnd() || src.charAt(current) != expected) return false;
        current++;
        column++;
        return true;
    }

    private char peek() { return isAtEnd() ? '\0' : src.charAt(current); }
    private char peekNext() { return current + 1 >= src.length() ? '\0' : src.charAt(current + 1); }
    private boolean isAtEnd() { return current >= src.length(); }

    private char advance() {
        char c = src.charAt(current);
        current++;
        column++;
        return c;
    }

    private void addToken(TokenType type, Object literal) {
        String text = src.substring(start, current);
        tokens.add(new Token(type, text, literal, line, column - (current - start)));
    }

    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private static boolean isAlpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private static boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }

    private RuntimeException error(String msg) {
        return new RuntimeException("[词法错误] 第 " + line + " 行: " + msg);
    }
}
