package com.example;

import com.example.spi.SPISayService;

import java.util.ServiceLoader;

public class Main {

    public static void main(String[] args) {
        ServiceLoader<SPISayService> service = ServiceLoader.load(SPISayService.class);
        for (SPISayService spiSayService : service) {
            spiSayService.sayHello();
        }
    }
}
