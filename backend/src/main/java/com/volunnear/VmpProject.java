package com.volunnear;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Endpoints library", version = "1.0", description = "Endpoints library of the vmp-project API"))
public class VmpProject {
    public static void main(String[] args) {
        SpringApplication.run(VmpProject.class, args);
    }
}