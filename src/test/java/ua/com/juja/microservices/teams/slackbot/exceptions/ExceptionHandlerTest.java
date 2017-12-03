package ua.com.juja.microservices.teams.slackbot.exceptions;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.controller.TeamSlackbotController;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;
import ua.com.juja.microservices.teams.slackbot.service.UserService;
import ua.com.juja.microservices.teams.slackbot.util.SlackIdHandler;
import ua.com.juja.microservices.utils.TestUtils;

import javax.inject.Inject;
import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@WebMvcTest(TeamSlackbotController.class)
public class ExceptionHandlerTest {
    private static String ACTIVATE_TEAM_MESSAGE = "Thanks, Activate Team job started!";
    private String teamsSlackbotActivateTeamUrl = "/v1/commands/teams/activate";
    @Inject
    private MockMvc mvc;
    @MockBean
    private TeamService teamService;
    @MockBean
    private RestTemplate restTemplate;
    @MockBean
    private UserService userService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void handleTeamExchangeException() throws Exception {
        //given
        String from = "from-id";
        String activateTeamCommandText = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        String responseUrl = "example.com";
        String messageWithUuids = "User(s) '#uuid1,uuid2,uuid3,uuid4#' exist(s) in another teams";
        String messageWithSlackIds = String.format("User(s) '#%s %s %s %s#'  exist(s) in another teams",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        ApiError apiError = new ApiError(
                400, "TMF-F2-D3",
                "Sorry, but the user already exists in team!",
                "The reason of the exception is that user already in team",
                messageWithUuids,
                Collections.emptyList());
        TeamExchangeException exception = new TeamExchangeException(apiError, new RuntimeException("exception"));

        when(teamService.activateTeam(from, activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");
        when(userService.replaceUuidsBySlackIdsInExceptionMessage(messageWithUuids)).thenReturn(messageWithSlackIds);

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        //then
        verify(teamService).activateTeam(from, activateTeamCommandText);
        verify(userService).replaceUuidsBySlackIdsInExceptionMessage(messageWithUuids);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(messageWithSlackIds));
        verifyNoMoreInteractions(teamService, restTemplate, userService);
    }

    @Test
    public void handleUserExchangeException() throws Exception {
        //given
        String from = "from-id";
        String activateTeamCommandText = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        String responseUrl = "example.com";
        ApiError apiError = new ApiError(
                400, "USF-F1-D1",
                "User not found",
                "User not found",
                "Something went wrong",
                Collections.emptyList());
        UserExchangeException exception = new UserExchangeException(apiError, new RuntimeException("exception"));

        when(teamService.activateTeam(from, activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        //then
        verify(teamService).activateTeam(from, activateTeamCommandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(apiError.getExceptionMessage()));
        verifyNoMoreInteractions(teamService, restTemplate);
    }

    @Test
    public void handleWrongCommandFormatException() throws Exception {
        //given
        String from = "from-id";
        String activateTeamCommandText = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        String responseUrl = "example.com";
        WrongCommandFormatException exception = new WrongCommandFormatException("wrong command");

        when(teamService.activateTeam(from, activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        //then
        verify(teamService).activateTeam(from, activateTeamCommandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains("wrong command"));
        verifyNoMoreInteractions(teamService, restTemplate);
    }

    @Test
    public void handleResourceAccessException() throws Exception {
        //given
        String from = "from-id";
        String activateTeamCommandText = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        String responseUrl = "example.com";
        ResourceAccessException exception = new ResourceAccessException("Some service unavailable");

        when(teamService.activateTeam(from, activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        //then
        verify(teamService).activateTeam(from, activateTeamCommandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains("Some service unavailable"));
        verifyNoMoreInteractions(teamService, restTemplate);
    }

    @Test
    public void handleAllOtherExceptions() throws Exception {
        //given
        String from = "from-id";
        String activateTeamCommandText = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        String responseUrl = "example.com";
        RuntimeException exception = new RuntimeException("other command");

        when(teamService.activateTeam(from, activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        //then
        verify(teamService).activateTeam(from, activateTeamCommandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains("other command"));
        verifyNoMoreInteractions(teamService, restTemplate);
    }

    @Test
    public void handleNestedUserExceptionAfterTeamException() throws Exception {
        //given
        String from = "from-id";
        String activateTeamCommandText = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        String responseUrl = "example.com";
        String messageWithUuids = String.format("User(s) '#%s#' exist(s) in another teams",
                "uuid1,uuid2,uuid3,uuid4");
        ApiError apiError = new ApiError(
                400, "TMF-F2-D3",
                "Sorry, but the user already exists in team!",
                "The reason of the exception is that user already in team",
                messageWithUuids,
                Collections.emptyList());
        TeamExchangeException teamException = new TeamExchangeException(apiError, new RuntimeException("exception"));
        UserExchangeException userException = new UserExchangeException(apiError, new RuntimeException("exception"));

        when(teamService.activateTeam(from, activateTeamCommandText)).thenThrow(teamException);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");
        when(userService.replaceUuidsBySlackIdsInExceptionMessage(messageWithUuids)).thenThrow(userException);

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        //then
        verify(teamService).activateTeam(from, activateTeamCommandText);
        verify(userService).replaceUuidsBySlackIdsInExceptionMessage(anyString());
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(apiError.getClientMessage()));
        verifyNoMoreInteractions(teamService, restTemplate, userService);
    }

    @Test
    public void handleNestedOtherAfterTeamException() throws Exception {
        //given
        String from = "from-id";
        String activateTeamCommandText = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        String responseUrl = "example.com";
        String messageWithUuids = String.format("User(s) '#%s#' exist(s) in another teams",
                "uuid1,uuid2,uuid3,uuid4");
        String messageWithSlackIds = String.format("User(s) '#%s,%s,%s,%s#' exist(s) in another teams",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        ApiError apiError = new ApiError(
                400, "TMF-F2-D3",
                "Sorry, but the user already exists in team!",
                "The reason of the exception is that user already in team",
                messageWithUuids,
                Collections.emptyList());
        TeamExchangeException teamException = new TeamExchangeException(apiError, new RuntimeException("exception"));
        Exception exception = new RuntimeException("Oops something went wrong :(");

        when(teamService.activateTeam(from, activateTeamCommandText)).thenThrow(teamException);
        when(userService.replaceUuidsBySlackIdsInExceptionMessage(messageWithUuids)).thenReturn(messageWithSlackIds);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenThrow(exception);

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        //then
        verify(teamService).activateTeam(from, activateTeamCommandText);
        verify(userService).replaceUuidsBySlackIdsInExceptionMessage(messageWithUuids);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(messageWithSlackIds));
        verifyNoMoreInteractions(teamService, restTemplate, userService);
    }
}