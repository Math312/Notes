package com.jllsq;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        Car car = (Car) context.getBean("car");
        String[] names = context.getBeanDefinitionNames();
        System.out.println(context.getDisplayName());
        System.out.println(car);
        System.out.println(context.containsBean("car"));
        for(String str:names) {
            System.out.println(str);
        }
    }
}

