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
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.users.UserUuidRequest;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;

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
@SpringBootTest({"eureka.client.enabled=false"})
public class RestUserRepositoryTest {
    private static User user1;
    private static User user2;
    private static User user3;
    private static User user4;
    @Rule
    final public ExpectedException expectedException = ExpectedException.none();
    @Inject
    private UserRepository userRepository;
    @MockBean
    private GatewayClient gatewayClient;

    @BeforeClass
    public static void oneTimeSetup() {
        user1 = new User("uuid1", "@slack1");
        user2 = new User("uuid2", "@slack2");
        user3 = new User("uuid3", "@slack3");
        user4 = new User("uuid4", "@slack4");
    }

    @Test
    public void findUsersBySlackNamesIfUserServerReturnsUsersCorrectly() throws IOException {
        List<String> slackNames = Arrays.asList("@slack1", "@slack2", "@slack3", "@slack4");
        List<User> expected = Arrays.asList(user1, user2, user3, user4);
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest = ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(expected);

        List<User> actual = userRepository.findUsersBySlackNames(slackNames);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual)
                    .as("expected not equals actual")
                    .isEqualTo(expected);
            soft.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .as("'captorUserSlackNameRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
        });
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void findUsersBySlackNamesIfUserServerReturnsFeignExceptionWithCorrectContent() throws IOException {
        List<String> slackNames = Arrays.asList("@slack1", "@slack2", "@slack3", "@slack4");
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
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest = ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        try {
            userRepository.findUsersBySlackNames(slackNames);
        } finally {
            Assertions.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .containsExactlyInAnyOrder((slackNames.toArray(new String[slackNames.size()])));
            verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
            verifyNoMoreInteractions(gatewayClient);
        }
    }

    @Test
    public void findUsersBySlackNamesIfUserServerReturnsFeignExceptionWithIncorrectContent() throws IOException {
        List<String> slackNames = Arrays.asList("@slack1", "@slack2", "@slack3", "@slack4");
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content: \n";
        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest = ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(
                containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        try {
            userRepository.findUsersBySlackNames(slackNames);
        } finally {
            Assertions.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .containsExactlyInAnyOrder((slackNames.toArray(new String[slackNames.size()])));
            verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
            verifyNoMoreInteractions(gatewayClient);
        }
    }

    @Test
    public void findUsersBySlackNamesIfUserServerReturnsFeignExceptionWithoutContent() throws IOException {
        List<String> slackNames = Arrays.asList("@slack1", "@slack2", "@slack3", "@slack4");
        String expectedJsonResponseBody = "";
        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest = ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(
                containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        try {
            userRepository.findUsersBySlackNames(slackNames);
        } finally {
            Assertions.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .containsExactlyInAnyOrder((slackNames.toArray(new String[slackNames.size()])));
            verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
            verifyNoMoreInteractions(gatewayClient);
        }
    }

    @Test
    public void findUsersByUuidsIfUserServerReturnsUserCorrectly() throws IOException {
        List<String> uuids = Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4");
        List<User> expected = Arrays.asList(user1, user2, user3, user4);
        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        when(gatewayClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(expected);

        List<User> actual = userRepository.findUsersByUuids(uuids);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual)
                    .as("expected not equals actual")
                    .isEqualTo(expected);
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));
        });
        verify(gatewayClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void findUsersByUuidsIfUserServerReturnsException() throws IOException {
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
        when(gatewayClient.findUsersByUuids(captorUserUuidRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        try {
            userRepository.findUsersByUuids(uuids);
        } finally {
            Assertions.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));
            verify(gatewayClient).findUsersByUuids(captorUserUuidRequest.capture());
            verifyNoMoreInteractions(gatewayClient);
        }
    }
}