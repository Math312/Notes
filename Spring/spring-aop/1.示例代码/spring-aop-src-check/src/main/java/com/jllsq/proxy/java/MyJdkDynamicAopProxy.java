package com.jllsq.proxy.java;

import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

@AllArgsConstructor
public class MyJdkDynamicAopProxy implements InvocationHandler {

    private Object source;

    private MethodInterceptor methodInterceptor;

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodInvocation methodInvocation = new MethodInvocationImpl(source,method,args);
        return methodInterceptor.invoke(methodInvocation);
    }
}
