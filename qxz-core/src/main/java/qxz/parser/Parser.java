package qxz.parser;

import qxz.ast.Ast.*;
import qxz.lexer.Token;
import qxz.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * QXZ 递归下降语法分析器。
 * 生成 AST 后交由 QZ 运行时执行。支持类 Java / C++ / C# 风格语法。
 *
 * 表达式优先级（从低到高）：
 *   assignment < or < and < equality < comparison < term < factor < unary < call/postfix
 */
public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // ============ 语句 ============

    private Stmt declaration() {
        try {
            return statement();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private Stmt statement() {
        Token t = peek();
        if (match(TokenType.LET)) return varDecl();
        if (match(TokenType.FUNC)) return funcDecl();
        if (match(TokenType.IF)) return ifStmt();
        if (match(TokenType.WHILE)) return whileStmt();
        if (match(TokenType.FOR)) return forStmt();
        if (match(TokenType.RETURN)) return returnStmt();
        if (match(TokenType.BREAK)) { consumeSemi(); return new Break(); }
        if (match(TokenType.CONTINUE)) { consumeSemi(); return new Continue(); }
        if (match(TokenType.LBRACE)) return block();
        if (t.type == TokenType.IDENTIFIER && peekNext().type == TokenType.IDENTIFIER) {
            // section 块：section name { ... }
            return sectionStmt();
        }
        return exprStmt();
    }

    private Stmt varDecl() {
        Token name = consume(TokenType.IDENTIFIER, "变量声明后需要变量名");
        Expr init = null;
        if (match(TokenType.ASSIGN)) init = expression();
        consumeSemi();
        return new VarDecl(name.lexeme, init);
    }

    private Stmt funcDecl() {
        Token name = consume(TokenType.IDENTIFIER, "函数声明后需要函数名");
        consume(TokenType.LPAREN, "函数名后需要 '('");
        List<String> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                params.add(consume(TokenType.IDENTIFIER, "参数名必须是标识符").lexeme);
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "参数列表后需要 ')'");
        consume(TokenType.LBRACE, "函数体前需要 '{'");
        List<Stmt> body = blockStatements();
        return new FuncDecl(name.lexeme, params, body);
    }

    private Stmt ifStmt() {
        consume(TokenType.LPAREN, "if 后需要 '('");
        Expr cond = expression();
        consume(TokenType.RPAREN, "if 条件后需要 ')'");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELIF)) {
            // elif 视为嵌套 if 的 else 分支
            elseBranch = new If(parseElifCondition(), null, null);
            // 简化：这里用递归方式处理 elif
            elseBranch = elifChain();
        } else if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }
        return new If(cond, thenBranch, elseBranch);
    }

    private Stmt elifChain() {
        // 当前已消费 ELIF
        consume(TokenType.LPAREN, "elif 后需要 '('");
        Expr cond = expression();
        consume(TokenType.RPAREN, "elif 条件后需要 ')'");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELIF)) elseBranch = elifChain();
        else if (match(TokenType.ELSE)) elseBranch = statement();
        return new If(cond, thenBranch, elseBranch);
    }

    private Expr parseElifCondition() { return null; }

    private Stmt whileStmt() {
        consume(TokenType.LPAREN, "while 后需要 '('");
        Expr cond = expression();
        consume(TokenType.RPAREN, "while 条件后需要 ')'");
        Stmt body = statement();
        return new While(cond, body);
    }

    private Stmt forStmt() {
        consume(TokenType.LPAREN, "for 后需要 '('");
        Stmt init = null;
        if (!check(TokenType.SEMICOLON)) {
            if (match(TokenType.LET)) init = varDeclNoSemi();
            else init = new ExprStmt(expression());
        }
        consume(TokenType.SEMICOLON, "for 初始化后需要 ';'");
        Expr cond = null;
        if (!check(TokenType.SEMICOLON)) cond = expression();
        consume(TokenType.SEMICOLON, "for 条件后需要 ';'");
        Expr inc = null;
        if (!check(TokenType.RPAREN)) inc = expression();
        consume(TokenType.RPAREN, "for 增量后需要 ')'");
        Stmt body = statement();
        return new For(init, cond, inc, body);
    }

    private Stmt varDeclNoSemi() {
        Token name = consume(TokenType.IDENTIFIER, "变量声明后需要变量名");
        Expr init = null;
        if (match(TokenType.ASSIGN)) init = expression();
        return new VarDecl(name.lexeme, init);
    }

    private Stmt returnStmt() {
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) value = expression();
        consumeSemi();
        return new Return(value);
    }

    private Stmt block() {
        List<Stmt> stmts = blockStatements();
        return new Block(stmts);
    }

    private List<Stmt> blockStatements() {
        List<Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            stmts.add(declaration());
        }
        consume(TokenType.RBRACE, "代码块后需要 '}'");
        return stmts;
    }

    private Stmt sectionStmt() {
        advance(); // 消费 section 关键字（第一个标识符）
        Token name = consume(TokenType.IDENTIFIER, "section 后需要名称");
        consume(TokenType.LBRACE, "section " + name.lexeme + " 后需要 '{'");
        List<SectionEntry> entries = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            Token key = consume(TokenType.IDENTIFIER, "section 条目需要键名");
            consume(TokenType.COLON, "section 条目需要 ':'");
            Expr value = expression();
            consumeSemi();
            entries.add(new SectionEntry(key.lexeme, value));
        }
        consume(TokenType.RBRACE, "section 块后需要 '}'");
        return new Section(name.lexeme, entries);
    }

    private Stmt exprStmt() {
        Expr expr = expression();
        consumeSemi();
        return new ExprStmt(expr);
    }

    // ============ 表达式 ============

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = orExpr();
        if (match(TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN,
                  TokenType.STAR_ASSIGN, TokenType.SLASH_ASSIGN)) {
            Token op = previous();
            Expr value = assignment();
            if (expr instanceof Variable) {
                return new Assign(((Variable) expr).name, op.type, value);
            }
            if (expr instanceof Index) {
                // list[i] = value 由解释器处理：暂不支持，抛错提示
                throw error(op, "暂不支持对下标赋值");
            }
            throw error(op, "赋值目标必须是变量");
        }
        return expr;
    }

    private Expr orExpr() {
        Expr expr = andExpr();
        while (match(TokenType.OR)) {
            Token op = previous();
            Expr right = andExpr();
            expr = new Binary(expr, op.type, right);
        }
        return expr;
    }

    private Expr andExpr() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token op = previous();
            Expr right = equality();
            expr = new Binary(expr, op.type, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQ, TokenType.NEQ)) {
            Token op = previous();
            Expr right = comparison();
            expr = new Binary(expr, op.type, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE)) {
            Token op = previous();
            Expr right = term();
            expr = new Binary(expr, op.type, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = previous();
            Expr right = factor();
            expr = new Binary(expr, op.type, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token op = previous();
            Expr right = unary();
            expr = new Binary(expr, op.type, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.MINUS, TokenType.NOT)) {
            Token op = previous();
            Expr right = unary();
            return new Unary(op.type, right);
        }
        return postfix();
    }

    private Expr postfix() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LPAREN)) {
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do { args.add(expression()); } while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "函数调用后需要 ')'");
                expr = new Call(expr, args);
            } else if (match(TokenType.LBRACKET)) {
                Expr index = expression();
                consume(TokenType.RBRACKET, "下标后需要 ']'");
                expr = new Index(expr, index);
            } else if (match(TokenType.INC, TokenType.DEC)) {
                Token op = previous();
                expr = new Assign(((Variable) expr).name,
                        op.type == TokenType.INC ? TokenType.PLUS_ASSIGN : TokenType.MINUS_ASSIGN,
                        new Literal(1));
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr primary() {
        if (match(TokenType.INT)) return new Literal((Integer) previous().literal);
        if (match(TokenType.DOUBLE)) return new Literal((Double) previous().literal);
        if (match(TokenType.STRING)) return new Literal(previous().literal);
        if (match(TokenType.TRUE)) return new Literal(true);
        if (match(TokenType.FALSE)) return new Literal(false);
        if (match(TokenType.NULL)) return new Literal(null);
        if (match(TokenType.IDENTIFIER)) return new Variable(previous().lexeme);
        if (match(TokenType.LPAREN)) {
            Expr expr = expression();
            consume(TokenType.RPAREN, "表达式后需要 ')'");
            return expr;
        }
        if (match(TokenType.LBRACKET)) {
            List<Expr> elements = new ArrayList<>();
            if (!check(TokenType.RBRACKET)) {
                do { elements.add(expression()); } while (match(TokenType.COMMA));
            }
            consume(TokenType.RBRACKET, "列表后需要 ']'");
            return new ListLiteral(elements);
        }
        throw error(peek(), "无法解析的表达式");
    }

    // ============ 工具 ============

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() { return peek().type == TokenType.EOF; }

    private Token peek() { return tokens.get(current); }

    private Token peekNext() { return tokens.get(Math.min(current + 1, tokens.size() - 1)); }

    private Token previous() { return tokens.get(current - 1); }

    private Token consume(TokenType type, String msg) {
        if (check(type)) return advance();
        throw error(peek(), msg);
    }

    private void consumeSemi() {
        consume(TokenType.SEMICOLON, "语句末尾需要 ';'");
    }

    private RuntimeException error(Token token, String msg) {
        return new RuntimeException("[语法错误] 第 " + token.line + " 行: " + msg);
    }
}
