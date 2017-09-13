package ua.com.juja.microservices.teams.slackbot.exceptions;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.controller.TeamSlackbotController;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;
import ua.com.juja.microservices.teams.slackbot.service.UserService;
import ua.com.juja.microservices.utils.SlackUrlUtils;

import javax.inject.Inject;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

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

    private final static String ACTIVATE_TEAM_MESSAGE = "Thanks, Activate Team job started!";
    private final User user1 = new User("uuid1", "@slack1");
    private final User user2 = new User("uuid2", "@slack2");
    private final User user3 = new User("uuid3", "@slack3");
    private final User user4 = new User("uuid4", "@slack4");

    @Value("${teams.slackbot.endpoint.activateTeam}")
    private String teamsSlackbotActivateTeamUrl;
    @Inject
    private MockMvc mvc;
    @MockBean
    private TeamService teamService;
    @Inject
    private ExceptionsHandler exceptionsHandler;
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

        final String activateTeamCommandText = "@slack1 @slack2 @slack3 @slack4";
        final String responseUrl = "example.com";
        String messageWithUuids = String.format("User(s) '#%s#' exist(s) in another teams", "uuid1,uuid2,uuid3,uuid4");
        String messageWithSlackNames = String.format("User(s) '#%s#' exist(s) in another teams",
                "@slack1,@slack2,@slack3,@slack4");
        ApiError apiError = new ApiError(
                400, "TMF-F2-D3",
                "Sorry, but the user already exists in team!",
                "The reason of the exception is that user already in team",
                messageWithUuids,
                Collections.emptyList()
        );

        TeamExchangeException exception = new TeamExchangeException(apiError, new RuntimeException("exception"));
        when(teamService.activateTeam(activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");
        when(userService.replaceUuidsBySlackNamesInExceptionMessage(messageWithUuids)).thenReturn(messageWithSlackNames);

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamService).activateTeam(activateTeamCommandText);
        verify(userService).replaceUuidsBySlackNamesInExceptionMessage(messageWithUuids);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(messageWithSlackNames));
        verifyNoMoreInteractions(teamService, restTemplate, userService);
    }

    @Test
    public void handleMultithreadingTeamExchangeException() throws Exception {
        //given
        final String activateTeamCommandText1 = "@slack1 @slack2 @slack3 @slack4";
        final String activateTeamCommandText2 = "@slack5 @slack6 @slack7 @slack8";
        final String responseUrl1 = "example1.com";
        final String responseUrl2 = "example2.com";
        String messageWithUuids1 = String.format("User(s) '#%s#' exist(s) in another teams",
                "uuid1,uuid2,uuid3,uuid4");
        String messageWithUuids2 = String.format("User(s) '#%s#' exist(s) in another teams",
                "uuid5,uuid6,uuid7,uuid8");
        String messageWithSlackNames1 = String.format("User(s) '#%s#' exist(s) in another teams",
                "@slack1,@slack2,@slack3,@slack4");
        String messageWithSlackNames2 = String.format("User(s) '#%s#' exist(s) in another teams",
                "@slack5,@slack6,@slack7,@slack8");
        ApiError apiError1 = new ApiError(
                400, "TMF-F2-D3",
                "Sorry, but the user already exists in team!",
                "The reason of the exception is that user already in team",
                messageWithUuids1,
                Collections.emptyList()
        );
        ApiError apiError2 = new ApiError(
                400, "TMF-F2-D3",
                "Sorry, but the user already exists in team!",
                "The reason of the exception is that user already in team",
                messageWithUuids2,
                Collections.emptyList()
        );

        TeamExchangeException exception1 = new TeamExchangeException(apiError1, new RuntimeException("exception1"));
        TeamExchangeException exception2 = new TeamExchangeException(apiError2, new RuntimeException("exception2"));

        when(teamService.activateTeam(activateTeamCommandText1)).thenThrow(exception1);
        when(restTemplate.postForObject(eq(responseUrl1), any(RichMessage.class), eq(String.class))).thenReturn("");
        when(userService.replaceUuidsBySlackNamesInExceptionMessage(messageWithUuids1)).thenReturn(messageWithSlackNames1);

        when(teamService.activateTeam(activateTeamCommandText2)).then(invocation -> pauseThread(exception2));
        when(restTemplate.postForObject(eq(responseUrl2), any(RichMessage.class), eq(String.class))).thenReturn("");
        when(userService.replaceUuidsBySlackNamesInExceptionMessage(messageWithUuids2)).thenReturn(messageWithSlackNames2);

        //when
        Callable<Boolean> call = () -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                        SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate",
                                activateTeamCommandText2, responseUrl2))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .andExpect(status().isOk())
                        .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        };
        FutureTask<Boolean> task = new FutureTask<>(call);
        new Thread(task).start();
        Thread.sleep(2000);

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate",
                        activateTeamCommandText1, responseUrl1))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        Boolean SecondThreadIsFinished = task.get();

        //then
        InOrder teamServiceOrderVerifier = Mockito.inOrder(teamService);
        teamServiceOrderVerifier.verify(teamService).activateTeam(activateTeamCommandText2);
        teamServiceOrderVerifier.verify(teamService).activateTeam(activateTeamCommandText1);

        InOrder userServiceOrderVerifier = Mockito.inOrder(userService);
        userServiceOrderVerifier.verify(userService).replaceUuidsBySlackNamesInExceptionMessage(messageWithUuids1);
        userServiceOrderVerifier.verify(userService).replaceUuidsBySlackNamesInExceptionMessage(messageWithUuids2);

        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);

        InOrder restTemplateOrderVerifier = Mockito.inOrder(restTemplate);
        restTemplateOrderVerifier.verify(restTemplate).postForObject(eq(responseUrl1), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(messageWithSlackNames1));
        restTemplateOrderVerifier.verify(restTemplate).postForObject(eq(responseUrl2), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(messageWithSlackNames2));

        verifyNoMoreInteractions(teamService, restTemplate, userService);
    }

    private Team pauseThread(TeamExchangeException exception) throws InterruptedException {
        Thread.sleep(5000);
        throw exception;
    }

    @Test
    public void handleUserExchangeException() throws Exception {
        final String activateTeamCommandText = "@a @b @c @d";
        final String responseUrl = "example.com";
        ApiError apiError = new ApiError(
                400, "USF-F1-D1",
                "User not found",
                "User not found",
                "Something went wrong",
                Collections.emptyList()
        );
        UserExchangeException exception = new UserExchangeException(apiError, new RuntimeException("exception"));
        when(teamService.activateTeam(activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamService).activateTeam(activateTeamCommandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(apiError.getClientMessage()));
        verifyNoMoreInteractions(teamService, restTemplate);
    }

    @Test
    public void handleWrongCommandFormatException() throws Exception {
        final String activateTeamCommandText = "@a @b @c @d";
        final String responseUrl = "example.com";
        WrongCommandFormatException exception = new WrongCommandFormatException("wrong command");
        when(teamService.activateTeam(activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamService).activateTeam(activateTeamCommandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains("wrong command"));
        verifyNoMoreInteractions(teamService, restTemplate);
    }

    @Test
    public void handleResourceAccessException() throws Exception {
        final String activateTeamCommandText = "@a @b @c @d";
        final String responseUrl = "example.com";
        ResourceAccessException exception = new ResourceAccessException("Some service unavailable");
        when(teamService.activateTeam(activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamService).activateTeam(activateTeamCommandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains("Some service unavailable"));
        verifyNoMoreInteractions(teamService, restTemplate);
    }

    @Test
    public void handleAllOtherExceptions() throws Exception {
        final String activateTeamCommandText = "@a @b @c @d";
        final String responseUrl = "example.com";
        RuntimeException exception = new RuntimeException("other command");
        when(teamService.activateTeam(activateTeamCommandText)).thenThrow(exception);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamService).activateTeam(activateTeamCommandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains("other command"));
        verifyNoMoreInteractions(teamService, restTemplate);
    }

    @Test
    public void handleNestedUserExceptionAfterTeamException() throws Exception {
        final String activateTeamCommandText = "@a @b @c @d";
        final String responseUrl = "example.com";
        String messageWithUuids = String.format("User(s) '#%s#' exist(s) in another teams",
                "uuid1,uuid2,uuid3,uuid4");
        String messageWithSlackNames = String.format("User(s) '#%s#' exist(s) in another teams",
                "@slack1,@slack2,@slack3,@slack4");
        ApiError apiError = new ApiError(
                400, "TMF-F2-D3",
                "Sorry, but the user already exists in team!",
                "The reason of the exception is that user already in team",
                messageWithUuids,
                Collections.emptyList()
        );

        TeamExchangeException teamException = new TeamExchangeException(apiError, new RuntimeException("exception"));
        UserExchangeException userException = new UserExchangeException(apiError, new RuntimeException("exception"));
        when(teamService.activateTeam(activateTeamCommandText)).thenThrow(teamException);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");
        when(userService.replaceUuidsBySlackNamesInExceptionMessage(messageWithUuids)).thenThrow(userException);

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamService).activateTeam(activateTeamCommandText);
        verify(userService).replaceUuidsBySlackNamesInExceptionMessage(anyString());
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(apiError.getClientMessage()));
        verifyNoMoreInteractions(teamService, restTemplate, userService);
    }

    @Test
    public void handleNestedOtherAfterTeamException() throws Exception {
        final String activateTeamCommandText = "@a @b @c @d";
        final String responseUrl = "example.com";
        String messageWithUuids = String.format("User(s) '#%s#' exist(s) in another teams",
                "uuid1,uuid2,uuid3,uuid4");
        String messageWithSlackNames = String.format("User(s) '#%s#' exist(s) in another teams",
                "@slack1,@slack2,@slack3,@slack4");
        ApiError apiError = new ApiError(
                400, "TMF-F2-D3",
                "Sorry, but the user already exists in team!",
                "The reason of the exception is that user already in team",
                messageWithUuids,
                Collections.emptyList()
        );

        TeamExchangeException teamException = new TeamExchangeException(apiError, new RuntimeException("exception"));
        Exception exception = new RuntimeException("exception");
        when(teamService.activateTeam(activateTeamCommandText)).thenThrow(teamException);
        when(userService.replaceUuidsBySlackNamesInExceptionMessage(messageWithUuids)).thenReturn(messageWithSlackNames);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenThrow(exception);

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamService).activateTeam(activateTeamCommandText);
        verify(userService).replaceUuidsBySlackNamesInExceptionMessage(messageWithUuids);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(messageWithSlackNames));
        verifyNoMoreInteractions(teamService, restTemplate, userService);
    }
}