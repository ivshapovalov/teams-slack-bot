package ua.com.juja.microservices.teams.slackbot.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ua.com.juja.microservices.teams.slackbot.model.UserDTO;
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

    private UserDTO userFrom;
    private UserDTO user1;
    private UserDTO user2;
    private UserDTO user3;
    private UserDTO user4;

    @Before
    public void setup() {
        userFrom = new UserDTO("1", "@from-user");
        user1 = new UserDTO("2", "@slack1");
        user2 = new UserDTO("3", "@slack2");
        user3 = new UserDTO("4", "@slack3");
        user4 = new UserDTO("5", "@slack4");
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