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
import ua.com.juja.microservices.teams.slackbot.service.TeamService;
import ua.com.juja.microservices.teams.slackbot.util.SlackIdHandler;
import ua.com.juja.microservices.utils.TestUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
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

    private String teamsSlackbotActivateTeamUrl = "/v1/commands/teams/activate";
    private String teamsSlackbotDeactivateTeamUrl = "/v1/commands/teams/deactivate";
    private String teamsSlackbotGetTeamUrl = "/v1/commands/teams";
    private String teamsSlackbotGetMyTeamUrl = "/v1/commands/myteam";

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
        //givaen
        String commandText = SlackIdHandler.wrapSlackIdInFullPattern("slack-id1");
        String responseUrl = "http://example.com";
        List<String> urls = Arrays.asList(
                teamsSlackbotActivateTeamUrl,
                teamsSlackbotDeactivateTeamUrl,
                teamsSlackbotGetTeamUrl,
                teamsSlackbotGetMyTeamUrl);

        //when
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

        //then
        verify(exceptionsHandler, times(4)).setResponseUrl(responseUrl);
        verifyNoMoreInteractions(teamService, exceptionsHandler);
    }

    @Test
    public void onReceiveAllSlashCommandsWhenAnyParamIsNullShouldReturnSorryMessage() throws Exception {
        //given
        String commandText = SlackIdHandler.wrapSlackIdInFullPattern("slack-id1");
        String responseUrl = "";
        List<String> urls = Arrays.asList(
                teamsSlackbotActivateTeamUrl,
                teamsSlackbotDeactivateTeamUrl,
                teamsSlackbotGetTeamUrl,
                teamsSlackbotGetMyTeamUrl);

        //when
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

        //then
        verify(exceptionsHandler, times(4)).setResponseUrl(responseUrl);
        verifyNoMoreInteractions(teamService, exceptionsHandler);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String from = "from-id";
        String commandText = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id3"),
                SlackIdHandler.wrapSlackIdInFullPattern("slack-id4"));
        String responseUrl = "http://example.com";
        Set<String> slackIds = new LinkedHashSet<>(Arrays.asList("slack-id1", "slack-id2", "slack-id3", "slack-id4"));

        when(teamService.activateTeam(from, commandText)).thenReturn(slackIds);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).activateTeam(from, commandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE, commandText)));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String commandText = SlackIdHandler.wrapSlackIdInFullPattern("slack-id2");
        Set<String> slackIds = new LinkedHashSet<>(Arrays.asList("slack-id1", "slack-id2",
                "slack-id3", "slack-id4"));
        String responseUrl = "http://example.com";

        when(teamService.getTeam(commandText)).thenReturn(slackIds);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams",
                        commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE,
                        SlackIdHandler.wrapSlackIdInFullPattern("slack-id2"))));

        //then
        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).getTeam(commandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(GET_TEAM_DELAYED_MESSAGE, commandText,
                slackIds.stream().sorted()
                        .map(SlackIdHandler::wrapSlackIdInFullPattern)
                        .collect(Collectors.joining(" ")))));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String from = "from-id";
        String wrappedFrom = SlackIdHandler.wrapSlackIdInFullPattern("from-id");
        Set<String> slackIds = new LinkedHashSet<>(Arrays.asList("slack-id1", "slack-id2",
                "slack-id3", "slack-id4"));
        String wrappedSlackIdsAsText = slackIds.stream().sorted()
                .map(SlackIdHandler::wrapSlackIdInFullPattern)
                .collect(Collectors.joining(" "));
        String responseUrl = "http://example.com";

        when(teamService.getTeam(wrappedFrom)).thenReturn(slackIds);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/myteam",
                        from, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, wrappedFrom)));

        //then
        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).getTeam(wrappedFrom);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(GET_MY_TEAM_DELAYED_MESSAGE,
                wrappedFrom, wrappedSlackIdsAsText)));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String from = "from-id";
        String commandText = SlackIdHandler.wrapSlackIdInFullPattern("slack-id1");
        Set<String> slackIds = new LinkedHashSet<>(Collections.singletonList("slack1"));
        String responseUrl = "http://example.com";

        when(teamService.deactivateTeam(from, commandText)).thenReturn(slackIds);
        when(restTemplate.postForObject(eq(responseUrl), any(RichMessage.class), eq(String.class))).thenReturn("");

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/teams-deactivate",
                        commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE,
                        SlackIdHandler.wrapSlackIdInFullPattern("slack-id1"))));

        //then
        verify(exceptionsHandler).setResponseUrl(responseUrl);
        verify(teamService).deactivateTeam(from, commandText);
        ArgumentCaptor<RichMessage> captor = ArgumentCaptor.forClass(RichMessage.class);
        verify(restTemplate).postForObject(eq(responseUrl), captor.capture(), eq(String.class));
        assertTrue(captor.getValue().getText().contains(String.format(DEACTIVATE_TEAM_DELAYED_MESSAGE,
                slackIds.stream().sorted()
                        .map(SlackIdHandler::wrapSlackIdInFullPattern)
                        .collect(Collectors.joining(" ")))));
        verifyNoMoreInteractions(teamService, exceptionsHandler, restTemplate);
    }
}