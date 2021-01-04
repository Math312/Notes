package com.example.spi;

public class SayGuGuService implements SPISayService {
    @Override
    public void sayHello() {
        System.out.println("Gu Gu");
    }
}
