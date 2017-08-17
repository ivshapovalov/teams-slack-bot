package ua.com.juja.microservices.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import ua.com.juja.microservices.teams.slackbot.TeamSlackBotApplication;

import java.io.PrintStream;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DebugAroundMethodLoggerIntegrationTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private TeamSlackBotApplication application = new TeamSlackBotApplication();

    private PrintStream originalSystemOut;
    @Mock
    private PrintStream fakeSystemOut;

    @Before
    public void setUp() throws Exception {
        originalSystemOut = System.out;
        System.setOut(fakeSystemOut);
    }

    @After
    public void tearDown() throws Exception {
        System.setOut(originalSystemOut);
    }

    @Test
    public void test() throws Throwable {
        TeamSlackBotApplication.main(new String[0]);
        verify(System.out, times(1)).println(Matchers.matches("execution.*main.* "));
    }

//        @Test
//        public void testNegativeNumber() throws Throwable {
//            application.doSomething(-22);
//            verify(System.out, times(1)).println(matches("execution.*doSomething.* -22"));
//            verify(System.out, times(1)).println(matches("Doing something with number 22"));
//        }

//        @Test(expected = RuntimeException.class)
//        public void testPositiveLargeNumber() throws Throwable {
//            try {
//                application.doSomething(333);
//            }
//            catch (Exception e) {
//                verify(System.out, times(1)).println(matches("execution.*doSomething.* 333"));
//                verify(System.out, times(0)).println(matches("Doing something with number"));
//                assertEquals("oops", e.getMessage());
//                throw e;
//            }
//        }

}
