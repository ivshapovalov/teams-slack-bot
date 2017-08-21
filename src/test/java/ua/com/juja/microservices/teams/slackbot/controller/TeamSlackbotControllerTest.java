package ua.com.juja.microservices.teams.slackbot.controller;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.ExceptionsHandler;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;
import ua.com.juja.microservices.utils.SlackUrlUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
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
public class TeamSlackbotControllerTest {

    private final static String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command";
    private final static String ACTIVATE_TEAM_MESSAGE = "Thanks, Activate Team job started!";
    private final static String GET_TEAM_MESSAGE = "Thanks, Get Team for user '%s' job started!";
    private final static String GET_MY_TEAM_MESSAGE = "Thanks, Get My Team for user '%s' job started!";

    @Value("${teams.slackbot.rest.api.version}")
    private String teamsSlackbotRestApiVersion;
    @Value("${teams.slackbot.commandsUrl}")
    private String teamsSlackbotCommandsUrl;
    @Value("${teams.slackbot.endpoint.activateTeam}")
    private String teamsSlackbotActivateTeamUrl;
    @Value("${teams.slackbot.endpoint.getTeam}")
    private String teamsSlackbotGetTeamUrl;
    @Value("${teams.slackbot.endpoint.getMyTeam}")
    private String teamsSlackbotGetMyTeamUrl;

    private String teamsSlackbotFullActivateTeamUrl;
    private String teamsSlackBotFullGetTeamUrl;
    private String teamsSlackBotFullGetMyTeamUrl;

    @Inject
    private MockMvc mvc;

    @MockBean
    private TeamService teamSlackbotService;

    @MockBean
    private ExceptionsHandler exceptionsHandler;

    @MockBean
    private RestTemplate restTemplate;

    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @Before
    public void setup() {
        teamsSlackbotFullActivateTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotActivateTeamUrl;
        teamsSlackBotFullGetTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotGetTeamUrl;
        teamsSlackBotFullGetMyTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotGetMyTeamUrl;

        user1 = new User("1", "@slack1");
        user2 = new User("2", "@slack2");
        user3 = new User("3", "@slack3");
        user4 = new User("4", "@slack4");
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenIncorrectTokenShouldReturnSorryMessage() throws Exception {
        final String ACTIVATE_TEAM_COMMAND_TEXT = String.
                format(user1.getSlack(), user2.getSlack(), user3.getSlack(), user4.getSlack());

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullActivateTeamUrl),
                SlackUrlUtils.getUriVars("wrongSlackToken", "/command", ACTIVATE_TEAM_COMMAND_TEXT,
                        "http://example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(SORRY_MESSAGE));

        verify(exceptionsHandler).setResponseUrl(anyString());
        verifyNoMoreInteractions(teamSlackbotService, exceptionsHandler);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String ACTIVATE_TEAM_COMMAND_TEXT = String.
                format(user1.getSlack(), user2.getSlack(), user3.getSlack(), user4.getSlack());

        Set<String> members = new LinkedHashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid()));
        Team activatedTeam = new Team(members);
        String responseUrl = "http://example.com";
        when(teamSlackbotService.activateTeam(ACTIVATE_TEAM_COMMAND_TEXT))
                .thenReturn(activatedTeam);
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject()))
                .thenReturn("");
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-activate",
                        ACTIVATE_TEAM_COMMAND_TEXT, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_MESSAGE));

        verify(exceptionsHandler).setResponseUrl(anyString());
        verify(teamSlackbotService).activateTeam(ACTIVATE_TEAM_COMMAND_TEXT);
        verify(restTemplate).postForObject(anyString(), any(RichMessage.class), anyObject());
        verifyNoMoreInteractions(teamSlackbotService, exceptionsHandler, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenIncorrectTokenShouldReturnSorryMessage() throws Exception {
        final String commandText = user1.getSlack();

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackBotFullGetTeamUrl),
                SlackUrlUtils.getUriVars("wrongSlackToken", "/command", commandText,
                        "http://example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(SORRY_MESSAGE));

        verify(exceptionsHandler).setResponseUrl(anyString());
        verifyNoMoreInteractions(teamSlackbotService, exceptionsHandler);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = user2.getSlack();

        Set<String> slackNames = new LinkedHashSet<>(Arrays.asList(user1.getSlack(), user2.getSlack(),
                user3.getSlack(), user4.getSlack()));
        String responseUrl = "http://example.com";
        when(teamSlackbotService.getTeam(commandText)).thenReturn(slackNames);
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("");
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackBotFullGetTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams",
                        commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_MESSAGE, user2.getSlack())));

        verify(exceptionsHandler).setResponseUrl(anyString());
        verify(teamSlackbotService).getTeam(commandText);
        verify(restTemplate).postForObject(anyString(), any(RichMessage.class), anyObject());
        verifyNoMoreInteractions(teamSlackbotService, exceptionsHandler, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenIncorrectTokenShouldReturnSorryMessage() throws Exception {
        final String commandText = user1.getSlack();

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackBotFullGetMyTeamUrl),
                SlackUrlUtils.getUriVars("wrongSlackToken", "/command", commandText,
                        "http://example.com"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(SORRY_MESSAGE));

        verify(exceptionsHandler).setResponseUrl(anyString());
        verifyNoMoreInteractions(teamSlackbotService, exceptionsHandler);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String fromUser = "@from-user";

        Set<String> slackNames = new LinkedHashSet<>(Arrays.asList(user1.getSlack(), user2.getSlack(),
                user3.getSlack(), user4.getSlack()));
        String responseUrl = "http://example.com";
        when(teamSlackbotService.getTeam(fromUser)).thenReturn(slackNames);
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("");
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackBotFullGetMyTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/myteam",
                        fromUser, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_MESSAGE, fromUser)));

        verify(exceptionsHandler).setResponseUrl(anyString());
        verify(teamSlackbotService).getTeam(fromUser);
        verify(restTemplate).postForObject(anyString(), any(RichMessage.class), anyObject());
        verifyNoMoreInteractions(teamSlackbotService, exceptionsHandler, restTemplate);
    }

}