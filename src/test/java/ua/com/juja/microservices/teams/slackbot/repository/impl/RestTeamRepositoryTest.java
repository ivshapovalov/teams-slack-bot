package ua.com.juja.microservices.teams.slackbot.repository.impl;

import feign.FeignException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.teams.ActivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.DeactivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.Team;
import ua.com.juja.microservices.teams.slackbot.repository.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.repository.feign.TeamsClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest()
public class RestTeamRepositoryTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();
    @Inject
    private TeamRepository teamRepository;

    @MockBean
    private TeamsClient teamsClient;

    @Test
    public void activateTeamSendRequestToRemoteTeamsServerAndReturnActivatedTeamExecutedCorrectly() throws IOException {
        String uuidFrom = "uuid-from";
        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid5", "uuid2", "uuid3", "uuid4"));
        ActivateTeamRequest activateTeamRequest = new ActivateTeamRequest(uuidFrom, members);
        Team expected = new Team(members, uuidFrom, "id", new Date(), new Date());
        when(teamsClient.activateTeam(activateTeamRequest)).thenReturn(expected);

        Team actual = teamRepository.activateTeam(activateTeamRequest);

        assertEquals(expected, actual);
        verify(teamsClient).activateTeam(activateTeamRequest);
        verifyNoMoreInteractions(teamsClient);
    }

    @Test
    public void activateTeamSendRequestToRemoteTeamsServerWhichReturnsErrorThrowsException() throws IOException {
        String uuidFrom = "uuid-from";
        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        ActivateTeamRequest activateTeamRequest = new ActivateTeamRequest(uuidFrom, members);
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F1-D3\",\n" +
                        "  \"clientMessage\": \"Sorry, but the user already exists in team!\",\n" +
                        "  \"developerMessage\": \"The reason of the exception is that user already in team\",\n" +
                        "  \"exceptionMessage\": \"User(s) '#uuid1,uuid2,uuid3,uuid4#' exist(s) in another teams\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";

        FeignException feignException = mock(FeignException.class);
        when(teamsClient.activateTeam(activateTeamRequest)).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, but the user already exists in team"));

        try {
            teamRepository.activateTeam(activateTeamRequest);
        } finally {
            verify(teamsClient).activateTeam(activateTeamRequest);
            verifyNoMoreInteractions(teamsClient);
        }
    }

    @Test
    public void getTeamSendRequestToRemoteTeamsServerAndReturnTeamExecutedCorrectly() throws
            IOException {
        String uuid = "uuid5";
        String uuidFrom = "uuid-from";
        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid5", "uuid2", "uuid3", "uuid4"));
        Team expected = new Team(members, uuidFrom, "id", new Date(), new Date());
        when(teamsClient.getTeam(uuid)).thenReturn(expected);

        Team actual = teamRepository.getTeam(uuid);

        assertEquals(expected, actual);
        verify(teamsClient).getTeam(uuid);
        verifyNoMoreInteractions(teamsClient);
    }

    @Test
    public void getTeamSendRequestToRemoteTeamsServerWhichReturnsErrorThrowsException() throws IOException {
        String uuid = "uuid";
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F2-D2\",\n" +
                        "  \"clientMessage\": \"You cannot get/deactivate team if the user not a member of any team!\",\n" +
                        "  \"developerMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"exceptionMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";

        FeignException feignException = mock(FeignException.class);
        when(teamsClient.getTeam(uuid)).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("You cannot get/deactivate team if the user not a member of any team!"));

        try {
            teamRepository.getTeam(uuid);
        } finally {
            verify(teamsClient).getTeam(uuid);
            verifyNoMoreInteractions(teamsClient);
        }
    }

    @Test
    public void getTeamRemoteTeamsServerReturnsErrorWhichUnableToConvertToApiErrorThrowsTeamException() throws
            IOException {
        String uuid = "uuid";
        RuntimeException runtimeException =
                new RuntimeException("I'm, sorry. I cannot parse api error message from remote service :(");
        when(teamsClient.getTeam(uuid)).thenThrow(runtimeException);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        try {
            teamRepository.getTeam(uuid);
        } finally {
            verify(teamsClient).getTeam(uuid);
            verifyNoMoreInteractions(teamsClient);
        }
    }

    @Test
    public void deactivateTeamSendRequestToRemoteTeamsServerAndReturnDeactivatedTeamExecutedCorrectly() throws
            IOException {
        String uuidFrom = "uuid-from";
        String uuid = "uuid2";
        DeactivateTeamRequest deactivateTeamRequest = new DeactivateTeamRequest(uuidFrom, uuid);
        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        Team expected = new Team(members, uuidFrom, "id", new Date(), new Date());
        when(teamsClient.deactivateTeam(deactivateTeamRequest)).thenReturn(expected);

        Team actual = teamRepository.deactivateTeam(deactivateTeamRequest);

        assertEquals(expected, actual);
        verify(teamsClient).deactivateTeam(deactivateTeamRequest);
        verifyNoMoreInteractions(teamsClient);
    }

    @Test
    public void deactivateTeamSendRequestToRemoteTeamsServerWhichReturnsErrorThrowsException() throws IOException {
        String uuidFrom = "uuid-from";
        String uuid = "uuid2";
        DeactivateTeamRequest deactivateTeamRequest = new DeactivateTeamRequest(uuidFrom, uuid);
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F2-D2\",\n" +
                        "  \"clientMessage\": \"You cannot get/deactivate team if the user not a member of any team!\",\n" +
                        "  \"developerMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"exceptionMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";

        FeignException feignException = mock(FeignException.class);
        when(teamsClient.deactivateTeam(deactivateTeamRequest)).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("You cannot get/deactivate team if the user not a member of any team!"));

        teamRepository.deactivateTeam(deactivateTeamRequest);
        try {
            teamRepository.deactivateTeam(deactivateTeamRequest);
        } finally {
            verify(teamsClient).deactivateTeam(deactivateTeamRequest);
            verifyNoMoreInteractions(teamsClient);
        }
    }
}