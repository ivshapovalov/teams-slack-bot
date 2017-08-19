package ua.com.juja.microservices.teams.slackbot.aspect;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.BaseBotException;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Shapovalov
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LoggerFactory.class})
public class DebugAroundExceptionLoggerTest {
    @Mock
    private static Logger mockLogger;
    private static Map<LogLine, Integer> warnLogLines = new HashMap<>();
    private DebugAroundExceptionLogger debugAroundExceptionLogger = new DebugAroundExceptionLogger();
    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @BeforeClass
    public static void beforeClass() {

        PowerMockito.mockStatic(LoggerFactory.class);
        mockLogger = PowerMockito.mock(Logger.class);
        when(LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);
    }

    @AfterClass
    public static void checkLogsAfterClass() {
        warnLogLines.forEach((key, value) -> verify(mockLogger, times(value))
                .warn(key.getFormat(), key.getArg1(), key.getArg2()));
    }

    @Test
    public void logExceptionHandleMethodIfOneArgumentWhichIsBaseBotExceptionWithErrorExecutedCorrectly() throws Throwable {

        String exceptionMessage = "Some error";
        ApiError apiError = new ApiError(
                400, "USF-F1-D1",
                exceptionMessage,
                "User not found",
                "Something went wrong",
                Collections.emptyList()
        );

        BaseBotException exception = new UserExchangeException(apiError, new RuntimeException(exceptionMessage));
        Object result = new Object();
        Object[] args = {exception};
        String message = "execution(method)";
        String warnMessage = "{} called with args '{}'!";

        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.toShortString()).thenReturn(message);
        when(proceedingJoinPoint.proceed()).thenReturn(result);

        debugAroundExceptionLogger.logExceptionHandleMethods(proceedingJoinPoint);

        verify(proceedingJoinPoint).getArgs();
        verify(proceedingJoinPoint).toShortString();
        verify(proceedingJoinPoint).proceed();
        addLogLineToLogs(warnMessage, message.replace("execution", "exception"),
                exception.detailMessage());
        verifyNoMoreInteractions(proceedingJoinPoint);
    }

    @Test
    public void logExceptionHandleMethodsIfTwoArgumentsExecutedCorrectly() throws Throwable {

        Object result = new Object();
        Object[] args = {"Exception1", "Exception2"};
        String message = "execution(method)";
        String warnMessage = "{} called with args '{}'!";

        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.toShortString()).thenReturn(message);
        when(proceedingJoinPoint.proceed()).thenReturn(result);

        debugAroundExceptionLogger.logExceptionHandleMethods(proceedingJoinPoint);

        verify(proceedingJoinPoint).getArgs();
        verify(proceedingJoinPoint).toShortString();
        verify(proceedingJoinPoint).proceed();
        addLogLineToLogs(warnMessage, message.replace("execution", "exception"),
                Arrays.deepToString(args));
        verifyNoMoreInteractions(proceedingJoinPoint);
    }

    @Test
    public void logExceptionHandleMethodsIfOneArgumentNotBaseBotExceptionExecutedCorrectly() throws Throwable {

        Object result = new Object();
        Object[] args = {"Exception1"};
        String message = "execution(method)";
        String warnMessage = "{} called with args '{}'!";

        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.toShortString()).thenReturn(message);
        when(proceedingJoinPoint.proceed()).thenReturn(result);

        debugAroundExceptionLogger.logExceptionHandleMethods(proceedingJoinPoint);

        verify(proceedingJoinPoint).getArgs();
        verify(proceedingJoinPoint).toShortString();
        verify(proceedingJoinPoint).proceed();
        addLogLineToLogs(warnMessage, message.replace("execution", "exception"),
                Arrays.deepToString(args));
        verifyNoMoreInteractions(proceedingJoinPoint);
    }

    private void addLogLineToLogs(String format, Object arg1, Object arg2) {
        LogLine logLine = new LogLine(format, arg1, arg2);
        int count = warnLogLines.getOrDefault(logLine, 0);
        warnLogLines.put(logLine, count + 1);
    }

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    private class LogLine {
        private String format;
        private Object arg1;
        private Object arg2;
    }
}
