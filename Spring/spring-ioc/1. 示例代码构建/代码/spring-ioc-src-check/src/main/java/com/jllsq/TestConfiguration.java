package com.jllsq;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

    public TestConfiguration() {
        System.out.println("TestConfiguration is creating");
    }

    @Bean
    public Car car() {
        return new Car("Big Car",10);
    }
}
