package com.jllsq.proxy.java;

public class MakeDidiNoise implements MakeNoiseStrategy {

    private Object object;

    public MakeDidiNoise(Object o) {
        this.object = o;
    }

    public void makeNoise() {
        System.out.println(this.object+" say: DiDi");
    }
}
