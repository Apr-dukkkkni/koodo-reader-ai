package com.koodoagent.exception;

public class AgentException extends RuntimeException {
    private final String code;



    public AgentException(String code, String message) {
        super(message);
        this.code = code;
    }
//    RuntimeException 内部本身就有一个存错误信息detailMessage的成员变量，这个成员是 private，子类直接访问不到。
//    如果你不调用 super(message)，父类异常的message就是 null；
//            super(message) 就是把你传入的提示文字交给父类 RuntimeException，父类帮你存起来；
//    后续 e.getMessage()、打印堆栈日志的时候，才能拿到错误消息。

    public String getCode() {
        return code;
    }
}