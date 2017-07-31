package ua.com.juja.microservices.teams.slackbot.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SlackNameHandlerServiceTest {

    @Rule
    final public ExpectedException expectedException = ExpectedException.none();
    @Inject
    private SlackNameHandlerService slackNameHandlerService;
    @MockBean
    private UserService userService;
    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @Before
    public void setup() {
        user1 = new User("uuid1", "@slack1");
        user2 = new User("uuid2", "@slack2");
        user3 = new User("uuid3", "@slack3");
        user4 = new User("uuid4", "@slack4");
    }

    @Test
    public void getUuidsFromTextWithoutSlackInTextExecutedCorrectly() throws Exception {

        String text = "text without slack name TexT text.";
        List<String> requestToUserService = Arrays.asList(new String[]{});
        List<User> responseFromUserService = Arrays.asList();
        when(userService.findUsersBySlackNames(requestToUserService)).thenReturn(responseFromUserService);

        Set<String> actual = slackNameHandlerService.getUuidsFromText(text);

        assertThat(actual, is(Collections.emptySet()));
        verify(userService).findUsersBySlackNames(requestToUserService);
        verifyNoMoreInteractions(userService);
    }

    @Test
    public void getUuidsFromTextIfOneSlackInTextExecutedCorrectly() throws Exception {
        String text = "text " + user1.getSlack() + " TexT text.";
        List<String> requestToUserService = Arrays.asList(new String[]{user1.getSlack()});
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(new String[]{user1.getUuid()}));
        List<User> responseFromUserService = Arrays.asList(new User[]{user1});
        when(userService.findUsersBySlackNames(requestToUserService)).thenReturn(responseFromUserService);

        Set<String> actual = slackNameHandlerService.getUuidsFromText(text);

        assertThat(actual, is(expected));
        verify(userService).findUsersBySlackNames(requestToUserService);
        verifyNoMoreInteractions(userService);
    }

    @Test
    public void getUuidsFromTextIfFourSlackInTextExecutedCorrectly() throws Exception {

        String text = "text " + user1.getSlack() + " " + user2.getSlack() + " " + user3.getSlack() + " " +
                user4.getSlack();
        List<String> requestToUserService = Arrays.asList(new String[]{user1.getSlack(), user2.getSlack(),
                user3.getSlack(), user4.getSlack()});
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(new String[]{user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid()}));
        List<User> responseFromUserService = Arrays.asList(new User[]{user4, user2, user1, user3});
        when(userService.findUsersBySlackNames(requestToUserService)).thenReturn(responseFromUserService);

        Set<String> actual = slackNameHandlerService.getUuidsFromText(text);

        assertThat(actual, is(expected));
        verify(userService).findUsersBySlackNames(requestToUserService);
        verifyNoMoreInteractions(userService);
    }

    @Test
    public void getUuidsFromTextIfMembersIsNullThrowsException() throws Exception {
        String text = null;
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Text must not be null!");

        slackNameHandlerService.getUuidsFromText(text);

        verifyNoMoreInteractions(userService);
    }

    @Test
    public void getSlackNamesFromUuidsIfFourUuidsExecutedCorrectly() throws Exception {

        List<String> requestToUserService = Arrays.asList(new String[]{user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid()});
        Set<String> members = new LinkedHashSet<>(requestToUserService);
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(new String[]{user1.getSlack(), user2.getSlack(),
                user3.getSlack(), user4.getSlack()}));
        List<User> responseFromUserService = Arrays.asList(new User[]{user1, user2, user3, user4});
        when(userService.findUsersByUuids(requestToUserService)).thenReturn(responseFromUserService);

        Set<String> actual = slackNameHandlerService.getSlackNamesFromUuids(members);

        assertThat(actual, is(expected));
        verify(userService).findUsersByUuids(requestToUserService);
        verifyNoMoreInteractions(userService);
    }

    @Test
    public void getSlackNamesFromUuidsWithoutUuidsExecutedCorrectly() throws Exception {

        List<String> requestToUserService = Arrays.asList(new String[]{});
        Set<String> members = new LinkedHashSet<>();
        Set<String> expected = new LinkedHashSet<>();
        List<User> responseFromUserService = Arrays.asList();
        when(userService.findUsersByUuids(requestToUserService)).thenReturn(responseFromUserService);

        Set<String> actual = slackNameHandlerService.getSlackNamesFromUuids(members);

        assertThat(actual, is(expected));
        verify(userService).findUsersByUuids(requestToUserService);
        verifyNoMoreInteractions(userService);
    }

    @Test
    public void getSlackNamesFromUuidsIfMembersIsNullThrowsException() throws Exception {

        List<String> requestToUserService = Arrays.asList(new String[]{});
        Set<String> members = null;
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Team members must not be null!");

        slackNameHandlerService.getSlackNamesFromUuids(members);

        verify(userService).findUsersByUuids(requestToUserService);
        verifyNoMoreInteractions(userService);
    }

}