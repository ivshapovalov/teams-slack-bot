package ua.com.juja.microservices.teams.slackbot.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.service.TeamSlackbotService;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;

import javax.inject.Inject;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * @author Nikolay Horushko
 */
@RunWith(SpringRunner.class)
@WebMvcTest(TeamSlackbotController.class)
public class TeamSlackbotControllerTest {

    @Inject
    private MockMvc mvc;

    @MockBean
    private TeamSlackbotService teamSlackbotService;

    @MockBean
    private SlackNameHandlerService slackNameHandlerService;

    private User userFrom;
    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @Before
    public void setup() {
        userFrom = new User("1", "@from-user");
        user1 = new User("2", "@slack1");
        user2 = new User("3", "@slack2");
        user3 = new User("4", "@slack3");
        user4 = new User("5", "@slack4");
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenIncorrectTokenShouldReturnSorryRichMessage() throws Exception {
//        final String ACTIVATE_TEAM_COMMAND_TEXT = "@slack1 @slack2 @slack3 @slacl4";
//
//        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate("/commands/codenjoy"),
//                SlackUrlUtils.getUriVars("wrongSlackToken", "/command", CODENJOY_COMMAND_TEXT))
//                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.text").value(SORRY_MESSAGE));
    }

}