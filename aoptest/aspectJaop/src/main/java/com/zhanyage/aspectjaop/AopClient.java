package com.zhanyage.aspectjaop;

import android.util.Log;
import android.view.View;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.Arrays;

/**
 * Created by zhanyage on 2021/2/19
 * Describe: aop log code
 */
@Aspect
public class AopClient {

    private static final String TAG = "AopClient";

    private static final String ON_CLICK = "execution(* *.onClick(android.view.View))";
    private static final String ON_CREATE = "execution(* *.onCreate(android.os.Bundle))";
    private static final String ON_RESUME = "execution(* *.onResume(..))";
    private static final String ON_DESTROY = "execution(* *.onDestroy(..))";
    private static final String SUM_METHOD = "execution(private int com.zhanyage.aoptest.MainActivity.sum(int, int))";


    @Around(SUM_METHOD)
    public Object aroundSum(ProceedingJoinPoint pdj) {
        Object result = null;
        String methodName = pdj.getSignature().getName();
        System.out.println("前置通知方法>目标方法名：" + methodName + ",参数为：" + Arrays.asList(pdj.getArgs()));
        Log.i(TAG, "前置通知方法>目标方法名：" + methodName + ",参数为："  + Arrays.asList(pdj.getArgs()));
        try {
            result = pdj.proceed();
            Log.i(TAG,"返回通知方法>目标方法名" + methodName + ",返回结果为：" + result);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return result;
    }

    @Before(ON_CLICK)
    public void beforeOnClick(JoinPoint joinPoint) throws Throwable {
        try {
            String className = joinPoint.getTarget().getClass().getName();
            View view = (View)(joinPoint.getArgs())[0];
            Log.i(TAG, className + view.getId() + "is onClick");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before(ON_CREATE)
    public void beforeOnCreate(JoinPoint joinPoint) throws Throwable {
        try {
            String className = joinPoint.getTarget().getClass().getName();
            String method = joinPoint.getSignature().getName();
            Log.i(TAG, className + method + "onCreate is run");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before(ON_RESUME)
    public void beforeOnResume(JoinPoint joinPoint) throws Throwable {
        try {
            String className = joinPoint.getTarget().getClass().getName();
            String method = joinPoint.getSignature().getName();
            Log.i(TAG, className + method + "onResume is run");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before(ON_DESTROY)
    public void beforeOnDestroy(JoinPoint joinPoint) throws Throwable {
        try {
            String className = joinPoint.getTarget().getClass().getName();
            String method = joinPoint.getSignature().getName();
            Log.i(TAG, className + method + "onDestroy is run");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
