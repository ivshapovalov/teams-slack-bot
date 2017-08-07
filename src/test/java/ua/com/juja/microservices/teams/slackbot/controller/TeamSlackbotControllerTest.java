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

    @Value("${rest.api.version}")
    private String restApiVersion;

    private String ACTIVATE_TEAM_URL;

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
        ACTIVATE_TEAM_URL = "/" + restApiVersion + "/commands/teams/activate";

        user1 = new User("1", "@slack1");
        user2 = new User("2", "@slack2");
        user3 = new User("3", "@slack3");
        user4 = new User("4", "@slack4");
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenIncorrectTokenShouldReturnSorryMessage() throws Exception {
        final String ACTIVATE_TEAM_COMMAND_TEXT = String.
                format(user1.getSlack(), user2.getSlack(), user3.getSlack(), user4.getSlack());

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(ACTIVATE_TEAM_URL),
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
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(ACTIVATE_TEAM_URL),
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

}