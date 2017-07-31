package ua.com.juja.microservices.teams.slackbot.exceptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.controller.TeamSlackbotController;
import ua.com.juja.microservices.teams.slackbot.service.TeamSlackbotService;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;
import ua.com.juja.microservices.utils.SlackUrlUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
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

    @Value("${rest.api.version}")
    private String restApiVersion;

    private String ACTIVATE_TEAM_URL;

    @Inject
    private MockMvc mvc;

    @MockBean
    private TeamSlackbotService teamSlackbotService;

    @Inject
    private ExceptionsHandler exceptionsHandler;

    @MockBean
    private SlackNameHandlerService slackNameHandlerService;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
        ACTIVATE_TEAM_URL = "/" + restApiVersion + "/commands/teams/activate";
    }

    @Test
    public void handleTeamExchangeException() throws Exception {

        final String activateTeamCommandText = "@a @b @c @d";
        final String responseUrl = "example.com";
        String message = String.format("User(s) '#%s#' exist(s) in another teams",
                "a,b,c,d");
        Set<String> slackNames = new LinkedHashSet<>(Arrays.asList(new String[]{"@a", "@b", "@c", "@d"}));

        ApiError apiError = new ApiError(
                400, "TMF-F2-D2",
                "You cannot get/deactivate team if user in several teams",
                "The reason of the exception is that user in several teams",
                message,
                Collections.emptyList()
        );

        TeamExchangeException exception = new TeamExchangeException(apiError, new RuntimeException("exception"));
        when(teamSlackbotService.activateTeam(activateTeamCommandText))
                .thenThrow(exception);
        when(restTemplate.postForObject(anyString(), anyObject(), anyObject())).thenReturn("");
        when(slackNameHandlerService.getSlackNamesFromUuids(anySetOf(String.class))).thenReturn(slackNames);

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(ACTIVATE_TEAM_URL),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", activateTeamCommandText,
                        responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamSlackbotService).activateTeam(activateTeamCommandText);
        verify(slackNameHandlerService).getSlackNamesFromUuids(anySetOf(String.class));
        verify(restTemplate).postForObject(anyString(), anyObject(), anyObject());
        verifyNoMoreInteractions(teamSlackbotService, restTemplate);
    }

    @Test
    public void handleUserExchangeException() throws Exception {
        final String ACTIVATE_TEAM_COMMAND_TEXT = "@a @b @c @d";
        ApiError apiError = new ApiError(
                400, "USF-F1-D1",
                "User not found",
                "User not found",
                "Something went wrong",
                Collections.emptyList()
        );

        UserExchangeException exception = new UserExchangeException(apiError, new RuntimeException("exception"));
        when(teamSlackbotService.activateTeam(ACTIVATE_TEAM_COMMAND_TEXT))
                .thenThrow(exception);
        when(restTemplate.postForObject(anyString(), anyObject(), anyObject())).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(ACTIVATE_TEAM_URL),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", ACTIVATE_TEAM_COMMAND_TEXT,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamSlackbotService).activateTeam(ACTIVATE_TEAM_COMMAND_TEXT);
        verify(restTemplate).postForObject(anyString(), anyObject(), anyObject());
        verifyNoMoreInteractions(teamSlackbotService, restTemplate);
    }

    @Test
    public void handleWrongCommandFormatException() throws Exception {

        final String ACTIVATE_TEAM_COMMAND_TEXT = "@a @b @c @d";

        WrongCommandFormatException exception = new WrongCommandFormatException("wrong command");
        when(teamSlackbotService.activateTeam(ACTIVATE_TEAM_COMMAND_TEXT))
                .thenThrow(exception);
        when(restTemplate.postForObject(anyString(), anyObject(), anyObject())).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(ACTIVATE_TEAM_URL),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", ACTIVATE_TEAM_COMMAND_TEXT,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamSlackbotService).activateTeam(ACTIVATE_TEAM_COMMAND_TEXT);
        verify(restTemplate).postForObject(anyString(), anyObject(), anyObject());
        verifyNoMoreInteractions(teamSlackbotService, restTemplate);
    }

    @Test
    public void handleAllOtherException() throws Exception {

        final String ACTIVATE_TEAM_COMMAND_TEXT = "@a @b @c @d";

        RuntimeException exception = new RuntimeException("other command");
        when(teamSlackbotService.activateTeam(ACTIVATE_TEAM_COMMAND_TEXT))
                .thenThrow(exception);
        when(restTemplate.postForObject(anyString(), anyObject(), anyObject())).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(ACTIVATE_TEAM_URL),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate", ACTIVATE_TEAM_COMMAND_TEXT,
                        "example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(teamSlackbotService).activateTeam(ACTIVATE_TEAM_COMMAND_TEXT);
        verify(restTemplate).postForObject(anyString(), anyObject(), anyObject());
        verifyNoMoreInteractions(teamSlackbotService, restTemplate);
    }
}