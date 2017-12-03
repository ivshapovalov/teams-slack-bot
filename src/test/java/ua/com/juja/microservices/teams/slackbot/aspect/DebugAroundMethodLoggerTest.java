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
import org.powermock.api.mockito.PowerMockito;
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

        PowerMockito.mockStatic(LoggerFactory.class);
        mockLogger = PowerMockito.mock(Logger.class);
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
        //given
        Object expected = new Object();

        when(mockLogger.isDebugEnabled()).thenReturn(false);
        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        //when
        Object actual = debugAroundMethodLogger.logBusinessMethods(proceedingJoinPoint);

        //then
        verify(proceedingJoinPoint, times(1)).proceed();
        verifyNoMoreInteractions(proceedingJoinPoint);
        assertThat(actual, is(expected));
    }

    @Test
    public void logBusinessMethodsIfDebugEnabledAndMethodReturnsVoid() throws Throwable {
        //given
        Object result = Void.TYPE;
        MethodSignature methodSignature = PowerMockito.mock(MethodSignature.class, withSettings().extraInterfaces
                (Signature.class, MemberSignature.class, CodeSignature.class, MethodSignature.class));
        Object[] args = {"<@slack-id1>", "<@slack-id2>", "<@slack-id3>"};
        String returnType = "void";
        String message = "execution(method)";
        String beforeMessage = "{} called with args '{}'!";
        String afterMessage = "{} return '{}'!";

        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.toShortString()).thenReturn(message);
        when(proceedingJoinPoint.proceed()).thenReturn(result);
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getReturnType()).thenReturn(Void.TYPE);

        //when
        debugAroundMethodLogger.logBusinessMethods(proceedingJoinPoint);

        //then
        verify(proceedingJoinPoint).getArgs();
        verify(proceedingJoinPoint).toShortString();
        verify(proceedingJoinPoint).proceed();
        verify(proceedingJoinPoint).getSignature();
        verify(methodSignature).getReturnType();
        addLogLineToLogs(beforeMessage, message, Arrays.deepToString(args));
        addLogLineToLogs(afterMessage, message.replace("execution", "comeback"), returnType);
        verifyNoMoreInteractions(proceedingJoinPoint, methodSignature);
    }

    private void addLogLineToLogs(String format, Object arg1, Object arg2) {
        LogLine logLine = new LogLine(format, arg1, arg2);
        int count = debugLogLines.getOrDefault(logLine, 0);
        debugLogLines.put(logLine, count + 1);
    }

    @Test
    public void logBusinessMethodsIfDebugEnabledAndMethodReturnsNotVoid() throws Throwable {
        //given
        MethodSignature methodSignature = PowerMockito.mock(MethodSignature.class, withSettings().extraInterfaces
                (Signature.class, MemberSignature.class, CodeSignature.class, MethodSignature.class));
        Object[] args = {"<@slack-id1>", "<@slack-id2>", "<@slack-id3>"};
        String result = "@d";
        String message = "execution(method)";
        String beforeMessage = "{} called with args '{}'!";
        String afterMessage = "{} return '{}'!";

        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.toShortString()).thenReturn(message);
        when(proceedingJoinPoint.proceed()).thenReturn(result);
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getReturnType()).thenReturn(String.class);

        //when
        debugAroundMethodLogger.logBusinessMethods(proceedingJoinPoint);

        //then
        verify(proceedingJoinPoint).getArgs();
        verify(proceedingJoinPoint).toShortString();
        verify(proceedingJoinPoint).proceed();
        verify(proceedingJoinPoint).getSignature();
        verify(methodSignature).getReturnType();
        addLogLineToLogs(beforeMessage, message, Arrays.deepToString(args));
        addLogLineToLogs(afterMessage, message.replace("execution", "comeback"), result);
        verifyNoMoreInteractions(proceedingJoinPoint, methodSignature);
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
