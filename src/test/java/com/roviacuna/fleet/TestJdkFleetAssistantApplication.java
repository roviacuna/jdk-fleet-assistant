package com.roviacuna.fleet;

import org.springframework.boot.SpringApplication;

public class TestJdkFleetAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.from(JdkFleetAssistantApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
