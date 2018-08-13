package com.hand;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Profile;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.hand.infra.mapper")
@ServletComponentScan("com.hand.api.filter")
public class Application
{
    public static void main(String[] args)
    {
        SpringApplication.run(Application.class,args);
    }
}
