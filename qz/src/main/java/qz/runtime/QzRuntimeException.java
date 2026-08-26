package qz.runtime;

/**
 * QZ 运行时错误。携带行号便于定位。
 */
public class QzRuntimeException extends RuntimeException {
    private final int line;

    public QzRuntimeException(String message) {
        this(message, -1);
    }

    public QzRuntimeException(String message, int line) {
        super(message);
        this.line = line;
    }

    @Override
    public String getMessage() {
        return line >= 0 ? "[运行错误] 第 " + line + " 行: " + super.getMessage()
                         : "[运行错误] " + super.getMessage();
    }
}
