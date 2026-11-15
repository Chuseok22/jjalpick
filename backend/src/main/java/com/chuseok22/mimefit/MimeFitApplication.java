package com.chuseok22.mimefit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MimeFitApplication {

    public static void main(String[] args) {
        SpringApplication.run(MimeFitApplication.class, args);
    }
}
