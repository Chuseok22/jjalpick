package com.chuseok22.jjalpick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JjalPickApplication {

    public static void main(String[] args) {
        SpringApplication.run(JjalPickApplication.class, args);
    }
}
