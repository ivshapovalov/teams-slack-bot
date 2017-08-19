package ua.com.juja.microservices.teams.slackbot.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class DebugAroundMethodLogger {

    @Around("execution(* ua.com.juja.microservices.teams.slackbot..*.*(..))" +
            "&& !execution(* ua.com.juja.microservices.teams.slackbot.exceptions..*.*(..))")
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
}
