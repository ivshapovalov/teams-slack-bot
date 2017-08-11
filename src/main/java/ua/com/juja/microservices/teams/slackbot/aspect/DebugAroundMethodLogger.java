package ua.com.juja.microservices.teams.slackbot.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import ua.com.juja.microservices.teams.slackbot.exceptions.BaseBotException;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class DebugAroundMethodLogger {


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
            log.debug("{} called with args '{}'!", call.toShortString(), Arrays.deepToString(args));
            Object result = null;
            try {
                result = call.proceed();
                return result;
            } finally {
                Method method = ((MethodSignature) call.getSignature()).getMethod();
                if (method.getGenericReturnType() == Void.TYPE) {
                    result = "void";
                }
                String returnMessage = call.toShortString().replace("execution", "comeback");
                log.debug("{} return '{}'!", returnMessage, result);
            }
        }
    }
}
