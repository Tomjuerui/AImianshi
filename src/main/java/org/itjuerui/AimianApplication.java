package org.itjuerui;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.itjuerui.infra.repo")
public class AimianApplication {

    public static void main(String[] args) {
        SpringApplication.run(AimianApplication.class, args);
    }

}
