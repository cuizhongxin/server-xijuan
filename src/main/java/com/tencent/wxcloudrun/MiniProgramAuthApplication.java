package com.tencent.wxcloudrun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 微信小程序授权登录应用启动类
 */
@SpringBootApplication
@EnableScheduling
public class MiniProgramAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniProgramAuthApplication.class, args);
        System.out.println("\n=================================");
        System.out.println("微信小程序后端服务启动成功！");
        System.out.println("访问地址: http://localhost:3000");
        System.out.println("健康检查: http://localhost:3000/api/health");
        System.out.println("=================================\n");
    }
}


