package com.koodoagent.dto;

public record ErrorResponse(
        String code,
        String message,
        String requestId
) {
//    record 是 Java16+ 的语法：
//    括号里的 (String code, String message, String requestId) 是组件列表，自动生成：
//    全参构造器 new ErrorResponse(code,message,requestId)
//    三个访问方法：code()、message()、requestId()
//    equals / hashCode / toString
//👉 它本身只是一个数据载体，DTO，用来装数据；不是用来返回，是用来被构造出来，然后返回给前端。
//    // 传入三个字符串，构造出一个 ErrorResponse 对象
//    ErrorResponse resp = new ErrorResponse("LLM_FAIL", "解析JSON失败", "req‑123456");
//    "LLM_FAIL" 传给构造器的 code
//"解析JSON失败" 传给 message
//"req‑123456" 传给 requestId
//            new ErrorResponse(A,B,C)：你传入 A/B/C 三个字符串，在内存创建 ErrorResponse 实例对象
//return errorResp：SpringMVC 把这个 Java 对象交给 Jackson
//    Jackson 序列化 → JSON 字符串，通过 http 响应返回浏览器

}