package ua.com.juja.microservices.teams.slackbot.aspect;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MemberSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author Ivan Shapovalov
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LoggerFactory.class})
public class DebugAroundMethodLoggerTest {

    @Mock
    private static Logger mockLogger;
    private static Map<LogLine, Integer> debugLogLines = new HashMap<>();
    private DebugAroundMethodLogger debugAroundMethodLogger = new DebugAroundMethodLogger();

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @BeforeClass
    public static void beforeClass() {

        mockStatic(LoggerFactory.class);
        mockLogger = mock(Logger.class);
        when(LoggerFactory.getLogger(any(Class.class)))
                .thenReturn(mockLogger);
    }

    @AfterClass
    public static void checkLogsAfterClass() {
        debugLogLines.forEach((key, value) -> verify(mockLogger, times(value))
                .debug(key.getFormat(), key.getArg1(), key.getArg2()));
    }

    @Test
    public void logBusinessMethodsIfDebugDisabledDoNothing() throws Throwable {

        Object expected = new Object();
        when(mockLogger.isDebugEnabled()).thenReturn(false);
        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        Object actual = debugAroundMethodLogger.logBusinessMethods(proceedingJoinPoint);

        verify(proceedingJoinPoint, times(1)).proceed();
        verifyNoMoreInteractions(proceedingJoinPoint);
        assertThat(actual, is(expected));
    }

    @Test
    public void logBusinessMethodsIfDebugEnabledAndMethodReturnsVoid() throws Throwable {

        Object expected = Void.TYPE;
        MethodSignature methodSignature = mock(MethodSignature.class, withSettings().extraInterfaces
                (Signature.class, MemberSignature.class, CodeSignature.class, MethodSignature.class));
        Object[] args = {"@a", "@b", "@c"};
        String returnType = "void";
        String message = "execution(method)";
        String beforeMessage = "{} called with args '{}'!";
        String afterMessage = "{} return '{}'!";

        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.toShortString()).thenReturn(message);
        when(proceedingJoinPoint.proceed()).thenReturn(expected);
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getReturnType()).thenReturn(Void.TYPE);

        Object actual = debugAroundMethodLogger.logBusinessMethods(proceedingJoinPoint);

        verify(proceedingJoinPoint).getArgs();
        verify(proceedingJoinPoint).toShortString();
        verify(proceedingJoinPoint).proceed();
        verify(proceedingJoinPoint).getSignature();
        verify(methodSignature).getReturnType();
        addLogLineToLogs(beforeMessage, message, Arrays.deepToString(args));
        addLogLineToLogs(afterMessage, message.replace("execution", "comeback"), returnType);
        verifyNoMoreInteractions(proceedingJoinPoint, methodSignature);
        assertThat(actual, is(expected));
    }

    private void addLogLineToLogs(String format, Object arg1, Object arg2) {
        LogLine logLine = new LogLine(format, arg1, arg2);
        int count = debugLogLines.getOrDefault(logLine, 0);
        debugLogLines.put(logLine, count + 1);
    }

    @Test
    public void logBusinessMethodsIfDebugEnabledAndMethodReturnsNotVoid() throws Throwable {

        MethodSignature methodSignature = mock(MethodSignature.class, withSettings().extraInterfaces
                (Signature.class, MemberSignature.class, CodeSignature.class, MethodSignature.class));
        Object[] args = {"@a", "@b", "@c"};
        String expected = "@d";
        String message = "execution(method)";
        String beforeMessage = "{} called with args '{}'!";
        String afterMessage = "{} return '{}'!";

        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.toShortString()).thenReturn(message);
        when(proceedingJoinPoint.proceed()).thenReturn(expected);
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getReturnType()).thenReturn(String.class);

        Object actual = debugAroundMethodLogger.logBusinessMethods(proceedingJoinPoint);

        verify(proceedingJoinPoint).getArgs();
        verify(proceedingJoinPoint).toShortString();
        verify(proceedingJoinPoint).proceed();
        verify(proceedingJoinPoint).getSignature();
        verify(methodSignature).getReturnType();
        addLogLineToLogs(beforeMessage, message, Arrays.deepToString(args));
        addLogLineToLogs(afterMessage, message.replace("execution", "comeback"), expected);
        verifyNoMoreInteractions(proceedingJoinPoint, methodSignature);
        assertThat(actual, is(expected));
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
