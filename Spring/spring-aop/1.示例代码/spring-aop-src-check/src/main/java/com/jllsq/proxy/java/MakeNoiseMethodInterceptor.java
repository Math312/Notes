package com.jllsq.proxy.java;

public class MakeNoiseMethodInterceptor implements MethodInterceptor {

    public Object invoke(MethodInvocation methodInvocation) {
        System.out.println(methodInvocation.getTarget()+" say: start make noise!");
        Object result = methodInvocation.process();
        System.out.println(methodInvocation.getTarget()+" say: make noise completely!");
        if (Void.class.equals(methodInvocation.getMethod().getReturnType())) {
            return result;
        }
        return null;
    }
}
