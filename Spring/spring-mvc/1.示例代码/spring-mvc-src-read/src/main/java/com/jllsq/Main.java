package com.jllsq;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@ComponentScan
public class Main {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
    }

}
