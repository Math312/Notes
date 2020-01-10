package com.jllsq.proxy.java;

import lombok.Data;

@Data
public class ProxyConfig {

    private Class targetClass;

    private Object targetSource;

    private MethodInterceptor methodInterceptor;

}
