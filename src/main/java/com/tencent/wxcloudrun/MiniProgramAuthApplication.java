package com.tencent.wxcloudrun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;
import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class MiniProgramAuthApplication {

    @PostConstruct
    void setTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(MiniProgramAuthApplication.class, args);
        System.out.println("\n=================================");
        System.out.println("微信小程序后端服务启动成功！");
        System.out.println("时区: " + TimeZone.getDefault().getID());
        System.out.println("访问地址: http://localhost:3000");
        System.out.println("健康检查: http://localhost:3000/api/health");
        System.out.println("=================================\n");
    }
}


