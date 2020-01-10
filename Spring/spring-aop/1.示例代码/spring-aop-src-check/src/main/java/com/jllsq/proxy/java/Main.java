package com.jllsq.proxy.java;

public class Main {

    public static void main(String[] args) {
        ProxyConfig proxyConfig = new ProxyConfig();
        Car car = new Car();
         MakeNoiseStrategy didi = new MakeDidiNoise(car);
         MakeNoiseStrategy data = new MakeDaDaNoise(car);
        proxyConfig.setTargetSource(didi);
        proxyConfig.setTargetClass(MakeDidiNoise.class);
        proxyConfig.setMethodInterceptor(new MakeNoiseMethodInterceptor());
        ProxyFactory proxyFactory = new MyJdkDynamicProxyFactory();
        MakeNoiseStrategy makeNoiseStrategy = (MakeNoiseStrategy) proxyFactory.getProxy(proxyConfig);
        makeNoiseStrategy.makeNoise();

    }

}
