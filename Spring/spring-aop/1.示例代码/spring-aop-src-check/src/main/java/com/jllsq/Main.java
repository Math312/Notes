package com.jllsq;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MathConfiguration.class);
        MathCalculator cal = context.getBean(MathCalculator.class);
        System.out.println(cal.div(10, 2));
        context.close();
    }

}