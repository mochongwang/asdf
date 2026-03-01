package com.example.quant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 系统启动入口。
 *
 * <p>这里使用 Spring Boot 3.1，JDK 21 运行。</p>
 */
@SpringBootApplication
public class QuantApplication {

    /**
     * 主函数。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(QuantApplication.class, args);
    }
}
