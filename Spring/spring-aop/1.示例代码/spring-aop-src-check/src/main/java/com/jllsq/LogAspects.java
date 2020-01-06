package com.jllsq;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;

import java.util.Arrays;

@Aspect
public class LogAspects {

    @Pointcut("execution(public int com.jllsq.MathCalculator.div(int,int))")
    public void pointCut() {}

    @Before("pointCut()")
    public void logStart(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        System.out.println(joinPoint.getSignature().getDeclaringTypeName()+"除法运行..参数列表是：{"+ Arrays.asList(args)+"}");
    }
    @After("pointCut()")
    //JoinPoint必须放在参数表的第一位
    public void logEnd(JoinPoint joinPoint) {
        System.out.println(joinPoint.getSignature().getDeclaringTypeName()+"除法运行结束...");
    }
    @AfterReturning(value="pointCut()",returning="result")
    public void logReturn(JoinPoint joinPoint,int result) {
        System.out.println(joinPoint.getSignature().getDeclaringTypeName()+"除法运行..返回结果是：{}");
    }
    @AfterThrowing(value="pointCut()",throwing="expection")
    public void logException(JoinPoint joinPoint,Exception expection) {
        System.out.println(joinPoint.getSignature().getDeclaringTypeName()+"运行异常..异常信息是：{}"+expection);
    }
}