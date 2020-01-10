package com.jllsq.proxy.java;

import java.lang.reflect.Proxy;

public class MyJdkDynamicProxyFactory implements ProxyFactory {

    public Object getProxy(ProxyConfig config) {
        MyJdkDynamicAopProxy myJdkDynamicAopProxy = new MyJdkDynamicAopProxy(config.getTargetSource(),config.getMethodInterceptor());
        Class clazz = config.getTargetClass();
        Class[] interfaces = clazz.getInterfaces();
        return Proxy.newProxyInstance(config.getTargetClass().getClassLoader(),interfaces,myJdkDynamicAopProxy);
    }
}
