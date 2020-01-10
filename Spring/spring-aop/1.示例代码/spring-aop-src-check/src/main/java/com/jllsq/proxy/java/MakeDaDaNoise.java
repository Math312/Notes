package com.jllsq.proxy.java;

public class MakeDaDaNoise implements MakeNoiseStrategy {

    private Object object;

    public MakeDaDaNoise(Object o) {
        this.object = o;
    }

    public void makeNoise() {
        System.out.println(this.object+" say: DaDa");
    }
}
