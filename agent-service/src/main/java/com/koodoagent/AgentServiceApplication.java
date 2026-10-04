package com.koodoagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgentServiceApplication {

    public static void main(String[] args ) {

        SpringApplication.run(AgentServiceApplication.class, args);
    }
}