package com.example.spi;

public class SayHelloService implements SPISayService {
    public void sayHello() {
        System.out.println("Hello");
    }
}
