package qxz;

import qxz.ast.Ast;
import qxz.lexer.Lexer;
import qxz.lexer.Token;
import qxz.parser.LanguageHeader;
import qxz.parser.Parser;
import qz.runtime.Interpreter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * QXZ 命令行入口。
 *
 * 用法：
 *   java -cp build qxz.Main run <文件.qxz>     运行 QXZ 程序
 *   java -cp build qxz.Main lex <文件.qxz>     仅打印词法分析结果
 *   java -cp build qxz.Main ast <文件.qxz>     仅打印语法树
 *   java -cp build qxz.Main repl                进入交互式命令行
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }
        String cmd = args[0];
        try {
            switch (cmd) {
                case "run" -> {
                    if (args.length < 2) { printUsage(); return; }
                    runFile(args[1]);
                }
                case "lex" -> {
                    if (args.length < 2) { printUsage(); return; }
                    lexFile(args[1]);
                }
                case "ast" -> {
                    if (args.length < 2) { printUsage(); return; }
                    astFile(args[1]);
                }
                case "repl" -> repl();
                default -> printUsage();
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("QXZ 语言运行时 (QXZ + QZ)");
        System.out.println("用法:");
        System.out.println("  qxz run <文件.qxz>   运行 QXZ 程序");
        System.out.println("  qxz lex <文件.qxz>   打印词法分析结果");
        System.out.println("  qxz ast <文件.qxz>   打印语法树");
        System.out.println("  qxz repl             交互式命令行");
    }

    private static void runFile(String path) throws IOException {
        String source = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        LanguageHeader header = LanguageHeader.parse(source);
        if (header != null) {
            if (!"java".equalsIgnoreCase(header.language)) {
                System.err.println("[语法声明] QZ 未注册语法解析器: " + header);
                System.err.println("QXZ v1 内置 Java 风格语法；" + header.language
                        + " 语法需要 QZ 扩展模块支持");
                System.exit(1);
            }
            System.out.println("[语法声明] " + header + " → 使用 Java 风格解析器");
        }
        // 头部本身是 // 注释，词法层自动忽略，直接全文件解析
        List<Token> tokens = new Lexer(source).scanTokens();
        List<Ast.Stmt> statements = new Parser(tokens).parse();
        new Interpreter().execute(statements);
    }

    private static void lexFile(String path) throws IOException {
        String source = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        List<Token> tokens = new Lexer(source).scanTokens();
        for (Token t : tokens) {
            System.out.println(t.line + ":" + t.column + "  " + t);
        }
    }

    private static void astFile(String path) throws IOException {
        String source = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        List<Token> tokens = new Lexer(source).scanTokens();
        List<Ast.Stmt> statements = new Parser(tokens).parse();
        for (Ast.Stmt s : statements) {
            System.out.println(s.getClass().getSimpleName());
        }
        System.out.println("解析成功，共 " + statements.size() + " 条顶层语句");
    }

    private static void repl() {
        Interpreter interpreter = new Interpreter();
        System.out.println("QXZ REPL (输入 exit 退出)");
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        while (true) {
            System.out.print("qxz> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine();
            if (line.equals("exit")) break;
            if (line.isBlank()) continue;
            try {
                List<Token> tokens = new Lexer(line).scanTokens();
                List<Ast.Stmt> statements = new Parser(tokens).parse();
                interpreter.execute(statements);
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
