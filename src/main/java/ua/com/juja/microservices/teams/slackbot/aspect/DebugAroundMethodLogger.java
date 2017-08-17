package ua.com.juja.microservices.teams.slackbot.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class DebugAroundMethodLogger {

    protected static int counter= 0;

    @Pointcut("execution(* ua.com.juja.microservices.teams.slackbot..*.*(..))" +
            "&& !execution(* ua.com.juja.microservices.teams.slackbot.exceptions..*.*(..))")
    public void businessMethods() {
    }

    @Around("businessMethods()")
    public Object logBusinessMethods(ProceedingJoinPoint call) throws Throwable {
        if (!log.isDebugEnabled()) {
            return call.proceed();
        } else {
            Object[] args = call.getArgs();
            String message = call.toShortString();
            log.debug("{} called with args '{}'!", message, Arrays.deepToString(args));
            Object result = null;
            try {
                result = call.proceed();
                return result;
            } finally {
                MethodSignature methodSignature = (MethodSignature) call.getSignature();
                if (methodSignature.getReturnType() == Void.TYPE) {
                    result = "void";
                }
                String returnMessage = message.replace("execution", "comeback");
                log.debug("{} return '{}'!", returnMessage, result);
            }
        }
    }

//    @Before("businessMethods()")
//    public void logBefore(JoinPoint joinPoint) {
//        if (log.isDebugEnabled()) {
//            Object[] args = joinPoint.getArgs();
//            log.debug("{} called with args '{}'!", joinPoint.toShortString(), Arrays.deepToString(args));
//        }
//    }
//
//    @AfterReturning(value = "businessMethods()", returning = "returns")
//    public void logAfter(JoinPoint joinPoint, Object returns) {
//        if (log.isDebugEnabled()) {
//            String returnMessage = joinPoint.toShortString().replace("execution", "comeback");
//            log.debug("{} return value '{}'!",returnMessage, returns);
//        }
//    }
}
