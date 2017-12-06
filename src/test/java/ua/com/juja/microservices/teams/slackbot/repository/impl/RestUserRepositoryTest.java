package ua.com.juja.microservices.teams.slackbot.repository.impl;

import feign.FeignException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackRequest;
import ua.com.juja.microservices.teams.slackbot.model.users.UserUuidRequest;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;
import ua.com.juja.microservices.teams.slackbot.repository.feign.UsersClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RestUserRepositoryTest {

    private static User user1;
    private static User user2;
    private static User user3;
    private static User user4;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Inject
    private UserRepository userRepository;
    @MockBean
    private UsersClient usersClient;

    @BeforeClass
    public static void oneTimeSetup() {
        user1 = new User("uuid1", "slack1");
        user2 = new User("uuid2", "slack2");
        user3 = new User("uuid3", "slack3");
        user4 = new User("uuid4", "slack4");
    }

    @Test
    public void findUsersBySlackUsersIfUserServerReturnsUsersCorrectly() throws IOException {
        //given
        List<User> expected = Arrays.asList(user1, user2, user3, user4);
        List<String> slackUsers = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(),
                user3.getSlackUser(), user4.getSlackUser());
        ArgumentCaptor<UserSlackRequest> captorUserSlackUserRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);

        when(usersClient.findUsersBySlackUsers(captorUserSlackUserRequest.capture())).thenReturn(expected);

        //when
        List<User> actual = userRepository.findUsersBySlackUsers(slackUsers);

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual)
                    .as("expected not equals actual")
                    .isEqualTo(expected);
            soft.assertThat(captorUserSlackUserRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackUserRequest' slackUsers not contains 'slackUsers'")
                    .containsExactlyInAnyOrder(slackUsers.toArray(new String[slackUsers.size()]));
        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackUserRequest.capture());
        verifyNoMoreInteractions(usersClient);
    }

    @Test
    public void findUsersBySlackUsersIfUserServerReturnsFeignExceptionWithCorrectContent() throws IOException {
        //given
        List<String> slackUsers = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(),
                user3.getSlackUser(), user4.getSlackUser());
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F1-D3\",\n" +
                        "  \"clientMessage\": \"Sorry, User server return an error\",\n" +
                        "  \"developerMessage\": \"Exception - UserExchangeException\",\n" +
                        "  \"exceptionMessage\": \"Something wrong on User server\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";
        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackRequest> captorUserSlackUserRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);

        when(usersClient.findUsersBySlackUsers(captorUserSlackUserRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        try {
            //when
            userRepository.findUsersBySlackUsers(slackUsers);
        } finally {
            //then
            Assertions.assertThat(captorUserSlackUserRequest.getValue().getSlackUsers())
                    .containsExactlyInAnyOrder((slackUsers.toArray(new String[slackUsers.size()])));
            verify(usersClient).findUsersBySlackUsers(captorUserSlackUserRequest.capture());
            verifyNoMoreInteractions(usersClient);
        }
    }

    @Test
    public void findUsersBySlackUsersIfUserServerReturnsFeignExceptionWithIncorrectContent() throws IOException {
        //given
        List<String> slackUsers = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(),
                user3.getSlackUser(), user4.getSlackUser());
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content: \n";
        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackRequest> captorUserSlackUserRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);

        when(usersClient.findUsersBySlackUsers(captorUserSlackUserRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(
                containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        try {
            //when
            userRepository.findUsersBySlackUsers(slackUsers);
        } finally {
            //then
            Assertions.assertThat(captorUserSlackUserRequest.getValue().getSlackUsers())
                    .containsExactlyInAnyOrder((slackUsers.toArray(new String[slackUsers.size()])));
            verify(usersClient).findUsersBySlackUsers(captorUserSlackUserRequest.capture());
            verifyNoMoreInteractions(usersClient);
        }
    }

    @Test
    public void findUsersBySlackUsersIfUserServerReturnsFeignExceptionWithoutContent() throws IOException {
        //given
        List<String> slackUsers = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(),
                user3.getSlackUser(), user4.getSlackUser());
        String expectedJsonResponseBody = "";
        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackRequest> captorUserSlackUserRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);

        when(usersClient.findUsersBySlackUsers(captorUserSlackUserRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(
                containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        try {
            //when
            userRepository.findUsersBySlackUsers(slackUsers);
        } finally {
            //then
            Assertions.assertThat(captorUserSlackUserRequest.getValue().getSlackUsers())
                    .containsExactlyInAnyOrder((slackUsers.toArray(new String[slackUsers.size()])));
            verify(usersClient).findUsersBySlackUsers(captorUserSlackUserRequest.capture());
            verifyNoMoreInteractions(usersClient);
        }
    }

    @Test
    public void findUsersByUuidsIfUserServerReturnsUserCorrectly() throws IOException {
        List<String> uuids = Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4");
        List<User> expected = Arrays.asList(user1, user2, user3, user4);
        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        
        when(usersClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(expected);

        //when
        List<User> actual = userRepository.findUsersByUuids(uuids);

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual)
                    .as("expected not equals actual")
                    .isEqualTo(expected);
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));
        });
        verify(usersClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(usersClient);
    }

    @Test
    public void findUsersByUuidsIfUserServerReturnsException() throws IOException {
        //given
        List<String> uuids = Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4");
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F1-D3\",\n" +
                        "  \"clientMessage\": \"Sorry, User server return an error\",\n" +
                        "  \"developerMessage\": \"Exception - UserExchangeException\",\n" +
                        "  \"exceptionMessage\": \"Something wrong on User server\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";
        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);

        when(usersClient.findUsersByUuids(captorUserUuidRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        try {
            //when
            userRepository.findUsersByUuids(uuids);
        } finally {
            //then
            Assertions.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));
            verify(usersClient).findUsersByUuids(captorUserUuidRequest.capture());
            verifyNoMoreInteractions(usersClient);
        }
    }
}