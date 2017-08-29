package ua.com.juja.microservices.teams.slackbot.controller;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
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
    private final static String ACTIVATE_TEAM_INSTANT_MESSAGE = "Thanks, Activate Team job started!";
    private final static String ACTIVATE_TEAM_DELAYED_MESSAGE = "Thanks, new Team for '%s' activated!";
    private final static String GET_TEAM_INSTANT_MESSAGE = "Thanks, Get Team for user '%s' job started!";
    private final static String GET_TEAM_DELAYED_MESSAGE = "Thanks, Team for '%s' is '%s'!";
    private final static String GET_MY_TEAM_INSTANT_MESSAGE = "Thanks, Get My Team for user '%s' job started!";
    private final static String GET_MY_TEAM_DELAYED_MESSAGE = "Thanks, Team for '%s' is '%s'!";
    private final static String DEACTIVATE_TEAM_INSTANT_MESSAGE = "Thanks, Deactivate Team for user '%s' job started!";
    private final static String DEACTIVATE_TEAM_DELAYED_MESSAGE = "Thanks, Team '%s' deactivated!";

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
    @Value("${teams.slackbot.endpoint.deactivateTeam}")
    private String teamsSlackbotDeactivateTeamUrl;

    private String teamsSlackbotFullActivateTeamUrl;
    private String teamsSlackbotFullDeactivateTeamUrl;
    private String teamsSlackBotFullGetTeamUrl;
    private String teamsSlackBotFullGetMyTeamUrl;

    @Inject
    private MockMvc mvc;

    @MockBean
    private TeamService teamService;

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
        teamsSlackbotFullDeactivateTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotDeactivateTeamUrl;
        teamsSlackBotFullGetTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotGetTeamUrl;
        teamsSlackBotFullGetMyTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotGetMyTeamUrl;

        user1 = new User("uuid1", "@slack1");
        user2 = new User("uuid2", "@slack2");
        user3 = new User("uuid3", "@slack3");
        user4 = new User("uuid4", "@slack4");
    }

    @Test
    public void onReceiveAllSlashCommandsWhenIncorrectTokenShouldReturnSorryMessage() throws Exception {
        final String commandText = user1.getSlack();
        String responseUrl = "http://example.com";
        List<String> urls = Arrays.asList(
                teamsSlackbotFullActivateTeamUrl,
                teamsSlackbotFullDeactivateTeamUrl,
                teamsSlackBotFullGetTeamUrl,
                teamsSlackBotFullGetMyTeamUrl);
        urls.forEach(url -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(url),
                        SlackUrlUtils.getUriVars("wrongSlackToken", "/command", commandText, responseUrl))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .andExpect(status().isOk())
                        .andExpect(content().string(SORRY_MESSAGE));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        verify(exceptionsHandler, times(4)).setResponseUrl(responseUrl);
        verifyNoMoreInteractions(teamService, exceptionsHandler);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = String.format("%s %s %s %s", user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack());
        Set<String> members = new LinkedHashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid()));
        Team activatedTeam = new Team(members);
        String responseUrl = "http://example.com";
        when(teamService.activateTeam(commandText)).thenReturn(activatedTeam);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).activateTeam(commandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE, commandText)));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = user2.getSlack();
        Set<String> slackNames = new LinkedHashSet<>(Arrays.asList(user1.getSlack(), user2.getSlack(),
                user3.getSlack(), user4.getSlack()));
        String responseUrl = "http://example.com";
        when(teamService.getTeam(commandText)).thenReturn(slackNames);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackBotFullGetTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams",
                        commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, user2.getSlack())));

        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).getTeam(commandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(GET_TEAM_DELAYED_MESSAGE, commandText,
                slackNames.stream().collect(Collectors.joining(" ")))));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String fromUser = "from-user";
        final String fromUserWithAt = "@from-user";
        Set<String> slackNames = new LinkedHashSet<>(Arrays.asList(user1.getSlack(), user2.getSlack(),
                user3.getSlack(), user4.getSlack()));
        String responseUrl = "http://example.com";
        when(teamService.getTeam(fromUserWithAt)).thenReturn(slackNames);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackBotFullGetMyTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/myteam",
                        fromUser, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, fromUserWithAt)));

        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).getTeam(fromUserWithAt);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(GET_MY_TEAM_DELAYED_MESSAGE,
                fromUserWithAt, slackNames.stream().collect(Collectors.joining(" ")))));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = user1.getSlack();
        Set<String> slackNames = new LinkedHashSet<>(Arrays.asList(user1.getSlack(), user2.getSlack(),
                user3.getSlack(), user4.getSlack()));
        String responseUrl = "http://example.com";
        when(teamService.deactivateTeam(commandText)).thenReturn(slackNames);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullDeactivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/teams-deactivate",
                        commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, user1.getSlack())));

        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).deactivateTeam(commandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(DEACTIVATE_TEAM_DELAYED_MESSAGE,
                slackNames.stream().collect(Collectors.joining(" ")))));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }
}