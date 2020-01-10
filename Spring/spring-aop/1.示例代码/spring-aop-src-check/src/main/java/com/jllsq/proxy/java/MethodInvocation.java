package com.jllsq.proxy.java;

import java.lang.reflect.Method;

public interface MethodInvocation {

    Object getTarget();

    Method getMethod();

    Object[] getArguments();

    Object process();

}
