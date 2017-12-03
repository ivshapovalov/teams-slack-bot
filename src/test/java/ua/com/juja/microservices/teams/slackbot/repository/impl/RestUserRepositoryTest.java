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
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackIdRequest;
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
        user1 = new User("uuid1", "slack-id1");
        user2 = new User("uuid2", "slack-id2");
        user3 = new User("uuid3", "slack-id3");
        user4 = new User("uuid4", "slack-id4");
    }

    @Test
    public void findUsersBySlackIdsIfUserServerReturnsUsersCorrectly() throws IOException {
        //given
        List<User> expected = Arrays.asList(user1, user2, user3, user4);
        List<String> slackIds = Arrays.asList(user1.getSlackId(), user2.getSlackId(),
                user3.getSlackId(), user4.getSlackId());
        ArgumentCaptor<UserSlackIdRequest> captorUserSlackIdRequest =
                ArgumentCaptor.forClass(UserSlackIdRequest.class);

        when(usersClient.findUsersBySlackIds(captorUserSlackIdRequest.capture())).thenReturn(expected);

        //when
        List<User> actual = userRepository.findUsersBySlackIds(slackIds);

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual)
                    .as("expected not equals actual")
                    .isEqualTo(expected);
            soft.assertThat(captorUserSlackIdRequest.getValue().getSlackIds())
                    .as("'captorUserSlackIdRequest' slackIds not contains 'slackIds'")
                    .containsExactlyInAnyOrder(slackIds.toArray(new String[slackIds.size()]));
        });
        verify(usersClient).findUsersBySlackIds(captorUserSlackIdRequest.capture());
        verifyNoMoreInteractions(usersClient);
    }

    @Test
    public void findUsersBySlackIdsIfUserServerReturnsFeignExceptionWithCorrectContent() throws IOException {
        //given
        List<String> slackIds = Arrays.asList(user1.getSlackId(), user2.getSlackId(),
                user3.getSlackId(), user4.getSlackId());
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
        ArgumentCaptor<UserSlackIdRequest> captorUserSlackIdRequest =
                ArgumentCaptor.forClass(UserSlackIdRequest.class);

        when(usersClient.findUsersBySlackIds(captorUserSlackIdRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        try {
            //when
            userRepository.findUsersBySlackIds(slackIds);
        } finally {
            //then
            Assertions.assertThat(captorUserSlackIdRequest.getValue().getSlackIds())
                    .containsExactlyInAnyOrder((slackIds.toArray(new String[slackIds.size()])));
            verify(usersClient).findUsersBySlackIds(captorUserSlackIdRequest.capture());
            verifyNoMoreInteractions(usersClient);
        }
    }

    @Test
    public void findUsersBySlackIdsIfUserServerReturnsFeignExceptionWithIncorrectContent() throws IOException {
        //given
        List<String> slackIds = Arrays.asList(user1.getSlackId(), user2.getSlackId(),
                user3.getSlackId(), user4.getSlackId());
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content: \n";
        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackIdRequest> captorUserSlackIdRequest =
                ArgumentCaptor.forClass(UserSlackIdRequest.class);

        when(usersClient.findUsersBySlackIds(captorUserSlackIdRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(
                containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        try {
            //when
            userRepository.findUsersBySlackIds(slackIds);
        } finally {
            //then
            Assertions.assertThat(captorUserSlackIdRequest.getValue().getSlackIds())
                    .containsExactlyInAnyOrder((slackIds.toArray(new String[slackIds.size()])));
            verify(usersClient).findUsersBySlackIds(captorUserSlackIdRequest.capture());
            verifyNoMoreInteractions(usersClient);
        }
    }

    @Test
    public void findUsersBySlackIdsIfUserServerReturnsFeignExceptionWithoutContent() throws IOException {
        //given
        List<String> slackIds = Arrays.asList(user1.getSlackId(), user2.getSlackId(),
                user3.getSlackId(), user4.getSlackId());
        String expectedJsonResponseBody = "";
        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackIdRequest> captorUserSlackIdRequest =
                ArgumentCaptor.forClass(UserSlackIdRequest.class);

        when(usersClient.findUsersBySlackIds(captorUserSlackIdRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(
                containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        try {
            //when
            userRepository.findUsersBySlackIds(slackIds);
        } finally {
            //then
            Assertions.assertThat(captorUserSlackIdRequest.getValue().getSlackIds())
                    .containsExactlyInAnyOrder((slackIds.toArray(new String[slackIds.size()])));
            verify(usersClient).findUsersBySlackIds(captorUserSlackIdRequest.capture());
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