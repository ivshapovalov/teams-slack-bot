package ua.com.juja.microservices.teams.slackbot.controller;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.ExceptionsHandler;
import ua.com.juja.microservices.teams.slackbot.model.teams.Team;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;
import ua.com.juja.microservices.utils.TestUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
@TestPropertySource(value = {"classpath:application.properties", "classpath:messages/message.properties"})
public class TeamSlackbotControllerTest {

    @Value("${message.sorry}")
    private String SORRY_MESSAGE;
    @Value("${message.activate.team.instant}")
    private String ACTIVATE_TEAM_INSTANT_MESSAGE;
    @Value("${message.activate.team.delayed}")
    private String ACTIVATE_TEAM_DELAYED_MESSAGE;
    @Value("${message.get.team.instant}")
    private String GET_TEAM_INSTANT_MESSAGE;
    @Value("${message.get.team.delayed}")
    private String GET_TEAM_DELAYED_MESSAGE;
    @Value("${message.get.my.team.instant}")
    private String GET_MY_TEAM_INSTANT_MESSAGE;
    @Value("${message.get.my.team.delayed}")
    private String GET_MY_TEAM_DELAYED_MESSAGE;
    @Value("${message.deactivate.team.instant}")
    private String DEACTIVATE_TEAM_INSTANT_MESSAGE;
    @Value("${message.deactivate.team.delayed}")
    private String DEACTIVATE_TEAM_DELAYED_MESSAGE;

    private String teamsSlackbotActivateTeamUrl="/v1/commands/teams/activate";
    private String teamsSlackbotDeactivateTeamUrl="/v1/commands/teams/deactivate";
    private String teamsSlackbotGetTeamUrl="/v1/commands/teams";
    private String teamsSlackbotGetMyTeamUrl="/v1/commands/myteam";

    @Inject
    private MockMvc mvc;

    @MockBean
    private TeamService teamService;

    @MockBean
    private ExceptionsHandler exceptionsHandler;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    public void onReceiveAllSlashCommandsWhenIncorrectTokenShouldReturnSorryMessage() throws Exception {
        final String commandText = "@slack1";
        String responseUrl = "http://example.com";
        List<String> urls = Arrays.asList(
                teamsSlackbotActivateTeamUrl,
                teamsSlackbotDeactivateTeamUrl,
                teamsSlackbotGetTeamUrl,
                teamsSlackbotGetMyTeamUrl);
        urls.forEach(url -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(url),
                        TestUtils.getUriVars("wrongSlackToken", "/command", commandText, responseUrl))
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
    public void onReceiveAllSlashCommandsWhenAnyParamIsNullShouldReturnSorryMessage() throws Exception {
        final String commandText = "@slack1";
        String responseUrl = "";
        List<String> urls = Arrays.asList(
                teamsSlackbotActivateTeamUrl,
                teamsSlackbotDeactivateTeamUrl,
                teamsSlackbotGetTeamUrl,
                teamsSlackbotGetMyTeamUrl);
        urls.forEach(url -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(url),
                        TestUtils.getUriVars("wrongSlackToken", "/command", commandText, responseUrl))
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
        String from = "@slack-from";
        final String commandText = "@slack1 @slack2 @slack3 @slack4";
        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        Team activatedTeam = new Team(members, "uuid-from", "id", new Date(), new Date());
        String responseUrl = "http://example.com";
        when(teamService.activateTeam(from, commandText)).thenReturn(activatedTeam);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).activateTeam(from, commandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE, commandText)));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = "@slack2";
        Set<String> slackNames = new LinkedHashSet<>(Arrays.asList("@slack1", "@slack2", "@slack3", "@slack4"));
        String responseUrl = "http://example.com";
        when(teamService.getTeam(commandText)).thenReturn(slackNames);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams",
                        commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, "@slack2")));

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
        final String from = "slack-from";
        final String fromUserWithAt = "@slack-from";
        Set<String> slackNames = new LinkedHashSet<>(Arrays.asList("@slack1", "@slack2", "@slack3", "@slack4"));
        String responseUrl = "http://example.com";
        when(teamService.getTeam(fromUserWithAt)).thenReturn(slackNames);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/myteam",
                        from, responseUrl))
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
        final String from = "@slack-from";
        final String commandText = "@slack1";
        Set<String> slackNames = new LinkedHashSet<>(Collections.singletonList("@slack1"));
        String responseUrl = "http://example.com";
        when(teamService.deactivateTeam(from, commandText)).thenReturn(slackNames);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams-deactivate",
                        commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, "@slack1")));

        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).deactivateTeam(from, commandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(DEACTIVATE_TEAM_DELAYED_MESSAGE,
                slackNames.stream().collect(Collectors.joining(" ")))));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }
}