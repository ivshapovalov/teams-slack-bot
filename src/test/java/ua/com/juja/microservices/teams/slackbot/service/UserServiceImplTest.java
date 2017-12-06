package ua.com.juja.microservices.teams.slackbot.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;
import ua.com.juja.microservices.teams.slackbot.util.SlackUserHandler;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceImplTest {

    @Inject
    private UserService userService;
    @MockBean
    private UserRepository userRepository;

    @Test
    public void findUsersBySlackUsersExecutedCorrectly() throws Exception {
        //given
        List<String> slackUsersRequest = Arrays.asList("slack1", "slack2");
        List<User> expected = Arrays.asList(new User("uuid1", "slack1"),
                new User("uuid2", "slack2"));

        given(userRepository.findUsersBySlackUsers(slackUsersRequest)).willReturn(expected);

        //when
        List<User> actual = userService.findUsersBySlackUsers(slackUsersRequest);

        //then
        assertThat(actual, is(expected));
        verify(userRepository).findUsersBySlackUsers(slackUsersRequest);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    public void findUsersByUuidsExecutedCorrectly() throws Exception {
        //given
        List<String> uuidsRequest = Arrays.asList("uuid1", "uuid2");
        List<User> expected = Arrays.asList(new User("uuid1", "slack1"),
                new User("uuid2", "slack2"));

        given(userRepository.findUsersByUuids(uuidsRequest)).willReturn(expected);

        //when
        List<User> actual = userService.findUsersByUuids(uuidsRequest);

        //then
        assertThat(actual, is(expected));
        verify(userRepository).findUsersByUuids(uuidsRequest);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    public void replaceUuidsBySlackUsersInExceptionMessageExecutedCorrectly() throws Exception {
        //given
        String exceptionMessage = String.format("User(s) '#%s#' exist(s) in another teams",
                "uuid1,uuid2,uuid3,uuid4");
        String expected = String.format("User(s) '%s,%s,%s,%s' exist(s) in another teams",
                SlackUserHandler.wrapSlackUserInFullPattern("slack1"),
                SlackUserHandler.wrapSlackUserInFullPattern("slack2"),
                SlackUserHandler.wrapSlackUserInFullPattern("slack3"),
                SlackUserHandler.wrapSlackUserInFullPattern("slack4"));
        List<User> users = Arrays.asList(new User("uuid1", "slack1"),
                new User("uuid2", "slack2"), new User("uuid3", "slack3"),
                new User("uuid4", "slack4"));

        given(userRepository.findUsersByUuids(anyListOf(String.class))).willReturn(users);

        //when
        String actual = userService.replaceUuidsBySlackUsersInExceptionMessage(exceptionMessage);

        //then
        assertThat(actual, is(expected));
        verify(userRepository).findUsersByUuids(anyListOf(String.class));
        verifyNoMoreInteractions(userRepository);
    }
}