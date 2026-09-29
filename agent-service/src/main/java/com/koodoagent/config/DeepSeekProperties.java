package com.koodoagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")     //会把配置文件中以 llm. 开头的配置自动映射到 record 的属性
public record DeepSeekProperties(   //使用java record把application。yml/环境变量里LLM。*配置自动注入到对象里边
                                    //代替一堆@Value("${}"). Java16 + 新语法，自动生成构造器、getter、toString、equals，不用手写 get/set
        String baseUrl,
        String apiKey,
        String model,
        int timeoutSeconds


) {}

//什么时候不能用 record，就要换回 @Data POJO
//下面情况你不能用 record，要写普通 class 加 @Data：
//项目 JDK < 16，没有 record 语法。
//需要修改对象字段（需要 setter）。比如 DTO，接收入参，后面要 set 字段改值。
//SpringBoot 版本很低，不支持 record 绑定@ConfigurationProperties。