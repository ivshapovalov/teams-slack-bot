package ua.com.juja.microservices.teams.slackbot.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import ua.com.juja.microservices.teams.slackbot.exceptions.BaseBotException;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class DebugAroundExceptionLogger {

    @Around("execution(* ua.com.juja.microservices.teams.slackbot.exceptions..*.handle*(..))")
    public Object logExceptionHandleMethods(ProceedingJoinPoint call) throws Throwable {
        Object[] args = call.getArgs();
        String error = Arrays.deepToString(args);
        if (args.length == 1) {
            Object argument = args[0];
            if (argument instanceof BaseBotException) {
                error = ((BaseBotException) argument).detailMessage();
            }
        }
        String returnMessage = call.toShortString().replace("execution", "exception");
        log.warn("{} called with args '{}'!", returnMessage, error);
        return call.proceed();
    }
}
