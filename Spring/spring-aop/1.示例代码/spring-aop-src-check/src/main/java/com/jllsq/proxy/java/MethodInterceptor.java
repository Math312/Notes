package com.jllsq.proxy.java;

public interface MethodInterceptor {

    Object invoke(MethodInvocation methodInvocation);

}
