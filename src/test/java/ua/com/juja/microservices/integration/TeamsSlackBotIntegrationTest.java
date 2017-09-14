package ua.com.juja.microservices.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.util.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.TeamSlackBotApplication;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.utils.SlackUrlUtils;
import ua.com.juja.microservices.utils.TestUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TeamSlackBotApplication.class})
@AutoConfigureMockMvc
@TestPropertySource(value = {"classpath:application.properties", "classpath:messages/message.properties"})
public class TeamsSlackBotIntegrationTest {

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

    @Value("${teams.slackbot.endpoint.activateTeam}")
    private String teamsSlackbotActivateTeamUrl;
    @Value("${teams.slackbot.endpoint.getTeam}")
    private String teamsSlackbotGetTeamUrl;
    @Value("${teams.slackbot.endpoint.getMyTeam}")
    private String teamsSlackbotGetMyTeamUrl;
    @Value("${teams.slackbot.endpoint.deactivateTeam}")
    private String teamsSlackbotDeactivateTeamUrl;

    @Value("${teams.endpoint.activateTeam}")
    private String teamsActivateTeamUrl;
    @Value("${teams.endpoint.deactivateTeam}")
    private String teamsDeactivateTeamUrl;
    @Value("${teams.endpoint.getTeam}")
    private String teamsGetTeamUrl;

    @Value("${users.endpoint.usersBySlackNames}")
    private String usersUrlFindUsersBySlackNames;
    @Value("${users.endpoint.usersByUuids}")
    private String usersUrlFindUsersByUuids;

    @Inject
    private RestTemplate restTemplate;

    @Inject
    private MockMvc mvc;
    private MockRestServiceServer mockServer;

    private User user1;
    private User user2;
    private User user3;
    private User user4;
    private User user5;

    @Before
    public void setup() {

        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        user1 = new User("uuid1", "@slack1");
        user2 = new User("uuid2", "@slack2");
        user3 = new User("uuid3", "@slack3");
        user4 = new User("uuid4", "@slack4");
        user5 = new User("uuid5", "@slack5");
    }

    @Test
    public void onReceiveAllSlashCommandsWhenTokenIsIncorrectShouldReturnErrorMessage() throws Exception {
        final String commandText = "@slack1";
        String responseUrl = "http:/example.com";
        List<String> urls = Arrays.asList(
                teamsSlackbotActivateTeamUrl,
                teamsSlackbotDeactivateTeamUrl,
                teamsSlackbotGetTeamUrl,
                teamsSlackbotGetMyTeamUrl);
        urls.forEach(url -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(url),
                        SlackUrlUtils.getUriVars("wrongToken", "/command", commandText, responseUrl))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .andExpect(status().isOk())
                        .andExpect(content().string(SORRY_MESSAGE));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void onReceiveAllSlashCommandsWhenParameterIsEmptyShouldReturnErrorMessage() throws Exception {
        final String commandText = user1.getSlack();
        String responseUrl = "";
        List<String> urls = Arrays.asList(
                teamsSlackbotActivateTeamUrl,
                teamsSlackbotDeactivateTeamUrl,
                teamsSlackbotGetTeamUrl,
                teamsSlackbotGetMyTeamUrl);
        urls.forEach(url -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(url),
                        SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .andExpect(status().isOk())
                        .andExpect(content().string(SORRY_MESSAGE));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = String.format("%s %s %s %s", user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack());
        final List<User> usersInCommand = Arrays.asList(user1, user2, user3, user4);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        String teamsJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        Set<String> uuids = new LinkedHashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid()));
        Team activatedTeam = new Team(uuids);
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.POST, teamsActivateTeamUrl, teamsJsonRequestBody,
                activatedTeam);
        mockSlackResponseUrl(responseUrl, new RichMessage(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE, commandText)));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));
    }

    @Test
    public void
    onReceiveSlashCommandActivateTeamWhenFindUsersBySlackNamesReturnsWrongUsersCountShouldReturnErrorMessage()
            throws Exception {
        final String commandText = String.format("%s %s %s %s", user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack());
        final List<User> usersInCommand = new ArrayList<>(Collections.singletonList(user1));
        String responseUrl = "http://example.com";
        mockFailUsersServiceFindUsersBySlackNamesReturnsWrongUsersNumber(usersInCommand);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Users count is not equals in request and response from Users Service"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));
    }

    @Test
    public void
    onReceiveSlashCommandActivateTeamWhenTeamsServiceRequestAndResponseContainsDifferentUuidsShouldReturnErrorMessage()
            throws Exception {
        final String commandText = String.format("%s %s %s %s", user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack());
        final List<User> usersInCommand = Arrays.asList(user1, user2, user3, user4);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        String teamsJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        Set<String> uuids = new LinkedHashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), "illegal-uuid"));
        Team activatedTeam = new Team(uuids);
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.POST, teamsActivateTeamUrl, teamsJsonRequestBody,
                activatedTeam);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Team members is not equals in request and response from Teams Service"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        final String commandText = String.format("%s %s %s %s", user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack());
        final List<User> usersInCommand = Arrays.asList(user1, user2, user3, user4);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        String teamsJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        String teamsJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryActivateTeamIfUsersInActiveTeamThrowsException.json"));
        mockFailTeamsServiceReturnsTeamException(HttpMethod.POST, teamsActivateTeamUrl, teamsJsonRequestBody,
                teamsJsonResponseBody);
        mockSuccessUsersServiceFindUsersByUuids(usersInCommand);
        mockSlackResponseUrl(responseUrl,
                new RichMessage(String.format("User(s) '%s' exist(s) in another teams",
                        usersInCommand.stream().
                                map(User::getSlack)
                                .collect(Collectors.joining(",")))));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenThreeSlackNamesInTextShouldReturnErrorMessage() throws
            Exception {
        final String commandText = String.format("%s %s %s ", user1.getSlack(), user2.getSlack(), user3.getSlack());
        final List<User> usersInCommand = Arrays.asList(user1, user2, user3);
        final int TEAM_SIZE = 4;
        String responseUrl = "http://example.com";
        mockSlackResponseUrl(responseUrl, new RichMessage(
                String.format("We found %d slack names in your command." +
                        " But size of the team must be %s.", usersInCommand.size(), TEAM_SIZE)));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenUserServiceReturnErrorShouldReturnErrorMessage()
            throws Exception {
        final String commandText = String.format("%s %s %s %s", user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack());
        final List<User> usersInCommand = Arrays.asList(user1, user2, user3, user4);
        String responseUrl = "http://example.com";
        mockFailUsersServiceFindUsersBySlackNamesReturnsError(usersInCommand);
        mockSlackResponseUrl(responseUrl, new RichMessage("very big and scare error"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = user1.getSlack();
        final List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        List<User> users = Arrays.asList(user1, user2, user3, user4);
        Team deactivatedTeam = new Team(users.stream().map(User::getUuid).collect(Collectors.toSet()));
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.PUT, teamsDeactivateTeamUrl + "/" + user1.getUuid(), "",
                deactivatedTeam);
        mockSuccessUsersServiceFindUsersByUuids(users);
        mockSlackResponseUrl(responseUrl, new RichMessage(
                String.format(DEACTIVATE_TEAM_DELAYED_MESSAGE,
                        users.stream().map(User::getSlack).collect(Collectors.joining(" ")))));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenMoreThanOneSlackNameInTextShouldReturnErrorMessage() throws
            Exception {
        final String commandText = String.format("%s %s %s ", user1.getSlack(), user2.getSlack(), user3.getSlack());
        String responseUrl = "http://example.com";
        mockSlackResponseUrl(responseUrl, new RichMessage(
                String.format("We found %d slack names in your command." +
                        " But expect one slack name.", 3)));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void
    onReceiveSlashCommandDeactivateTeamWhenFindUsersBySlackNamesReturnsWrongUsersCountShouldReturnErrorMessage()
            throws Exception {
        final String commandText = user1.getSlack();
        final List<User> usersInCommand = new ArrayList<>(Collections.singletonList(user1));
        String responseUrl = "http://example.com";
        mockFailUsersServiceFindUsersBySlackNamesReturnsWrongUsersNumber(usersInCommand);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Users count is not equals in request and response from Users Service"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void
    onReceiveSlashCommandDeactivateTeamWhenUserServiceFindUsersByUuidsReturnsWrongUsersCountShouldReturnErrorMessage()
            throws Exception {
        final String commandText = user1.getSlack();
        final List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        List<User> users = new ArrayList<>(Arrays.asList(user1, user2, user3, user4));
        Team team = new Team(users.stream().map(User::getUuid).collect(Collectors.toSet()));
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.PUT, teamsDeactivateTeamUrl + "/" + user1.getUuid(), "",
                team);
        mockFailUsersServiceFindUsersByUuidsReturnsWrongUsersNumber(users);
        mockSlackResponseUrl(responseUrl, new RichMessage(
                "Users count is not equals in request and response from Users Service"));
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        final String commandText = user1.getSlack();
        final List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        String teamsJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetAndDeactivateTeamIfUsersNotInActiveTeamThrowsException.json"));
        mockFailTeamsServiceReturnsTeamException(HttpMethod.PUT, teamsDeactivateTeamUrl + "/" + user1.getUuid(), "",
                teamsJsonResponseBody);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("You cannot get/deactivate team if the user not a member of any " +
                        "team!"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String fromUser = "@from-user";
        final List<User> usersInFromUser = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInFromUser);
        List<User> users = Arrays.asList(user1, user2, user3, user4);
        String slackNames = users.stream()
                .map(User::getSlack).collect(Collectors.joining(" "));
        Team team = new Team(users.stream().map(User::getUuid).collect(Collectors.toSet()));
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.GET, teamsGetTeamUrl + "/" + user1.getUuid(), "", team);
        mockSuccessUsersServiceFindUsersByUuids(users);
        mockSlackResponseUrl(responseUrl, new RichMessage(
                String.format(GET_MY_TEAM_DELAYED_MESSAGE,
                        fromUser, slackNames)));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", "", responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, fromUser)));
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenUserServiceReturnErrorShouldReturnErrorMessage()
            throws Exception {
        final String fromUser = "@from-user";
        final List<User> usersInFromUser = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInFromUser);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Oops something went wrong :("));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", fromUser, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, fromUser)));
    }

    @Test
    public void
    onReceiveSlashCommandGetMyTeamWhenUserServiceFindUsersBySlackNamesReturnsWrongUsersCountShouldReturnErrorMessage()
            throws Exception {
        final String fromUser = "@from-user";
        final List<User> usersInFromUser = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInFromUser);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Users count is not equals in request and response from Users Service"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", fromUser, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, fromUser)));
    }

    @Test
    public void
    onReceiveSlashCommandGetMyTeamWhenUserServiceFindUsersByUuidsReturnsWrongUsersCountShouldReturnErrorMessage()
            throws Exception {
        final String fromUser = "@from-user";
        final List<User> usersInFromUser = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInFromUser);
        List<User> users = new ArrayList<>(Arrays.asList(user1, user2, user3, user4));
        Team team = new Team(users.stream().map(User::getUuid).collect(Collectors.toSet()));
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.GET, teamsGetTeamUrl + "/" + user1.getUuid(), "",
                team);
        mockFailUsersServiceFindUsersByUuidsReturnsWrongUsersNumber(users);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Users count is not equals in request and response from Users Service"));
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", fromUser, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, fromUser)));
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        final String fromUser = "@from-user";
        final List<User> usersInFromUser = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInFromUser);
        String teamsJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetAndDeactivateTeamIfUsersNotInActiveTeamThrowsException.json"));
        mockFailTeamsServiceReturnsTeamException(HttpMethod.GET, teamsGetTeamUrl + "/" + user1.getUuid(), "",
                teamsJsonResponseBody);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("You cannot get/deactivate team if the user not a member of any team!"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", fromUser, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, fromUser)));
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = user1.getSlack();
        final List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        List<User> users = Arrays.asList(user1, user2, user3, user4);
        String slackNames = users.stream()
                .map(User::getSlack).collect(Collectors.joining(" "));
        Team team = new Team(users.stream().map(User::getUuid).collect(Collectors.toSet()));
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.GET, teamsGetTeamUrl + "/" + user1.getUuid(), "",
                team);
        mockSuccessUsersServiceFindUsersByUuids(users);
        mockSlackResponseUrl(responseUrl, new RichMessage(
                String.format(GET_TEAM_DELAYED_MESSAGE,
                        commandText, slackNames)));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenMoreThanOneSlackNameInTextShouldReturnErrorMessage() throws
            Exception {
        final String commandText = String.format("%s %s %s ", user1.getSlack(), user2.getSlack(), user3.getSlack());
        String responseUrl = "http://example.com";
        mockSlackResponseUrl(responseUrl, new RichMessage(
                String.format("We found %d slack names in your command." +
                        " But expect one slack name.", 3)));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenUserServiceReturnErrorShouldReturnErrorMessage()
            throws Exception {
        final String commandText = user1.getSlack();
        final List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockFailUsersServiceFindUsersBySlackNamesReturnsError(usersInCommand);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("very big and scare error"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void
    onReceiveSlashCommandGetTeamWhenUserServiceFindUsersBySlackNamesReturnsWrongUsersCountShouldReturnErrorMessage()
            throws Exception {
        final String commandText = user1.getSlack();
        final List<User> users = new ArrayList<>(Collections.singletonList(user1));
        String responseUrl = "http://example.com";
        mockFailUsersServiceFindUsersBySlackNamesReturnsWrongUsersNumber(users);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Users count is not equals in request and response from Users Service"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void
    onReceiveSlashCommandGetTeamWhenUserServiceFindUsersByUuidsReturnsWrongUsersCountShouldReturnErrorMessage()
            throws Exception {
        final String commandText = user1.getSlack();
        final List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        List<User> users = new ArrayList<>(Arrays.asList(user1, user2, user3, user4));
        Team team = new Team(users.stream().map(User::getUuid).collect(Collectors.toSet()));
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.GET, teamsGetTeamUrl + "/" + user1.getUuid(), "",
                team);
        mockFailUsersServiceFindUsersByUuidsReturnsWrongUsersNumber(users);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Users count is not equals in request and response from Users Service"));
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        final String commandText = user1.getSlack();
        final List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        String teamsJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetAndDeactivateTeamIfUsersNotInActiveTeamThrowsException.json"));
        mockFailTeamsServiceReturnsTeamException(HttpMethod.GET, teamsGetTeamUrl + "/" + user1.getUuid(), "",
                teamsJsonResponseBody);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("You cannot get/deactivate team if the user not a member of any team!"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));
    }

    private void mockFailUsersServiceFindUsersBySlackNamesReturnsError(List<User> users) throws
            JsonProcessingException {
        List<String> slackNames = new ArrayList<>();
        for (User user : users) {
            slackNames.add(user.getSlack());
        }
        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(usersUrlFindUsersBySlackNames))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(MockRestRequestMatchers.content().string(String.format("{\"slackNames\":%s}", mapper.writeValueAsString(slackNames))))
                .andRespond(withBadRequest().body("{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}"));
    }

    private void mockFailUsersServiceFindUsersBySlackNamesReturnsWrongUsersNumber(List<User> users) throws
            JsonProcessingException {
        List<String> slackNames = new ArrayList<>();
        for (User user : users) {
            slackNames.add(user.getSlack());
        }
        users.add(user5);

        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(usersUrlFindUsersBySlackNames))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(MockRestRequestMatchers.content().string(String.format("{\"slackNames\":%s}", mapper.writeValueAsString(slackNames))))
                .andRespond(withSuccess(mapper.writeValueAsString(users), MediaType.APPLICATION_JSON_UTF8));
    }

    private void mockSuccessUsersServiceFindUsersBySlackNames(List<User> users) throws JsonProcessingException {
        List<String> slackNames = new ArrayList<>();
        for (User user : users) {
            slackNames.add(user.getSlack());
        }
        ObjectMapper mapper = new ObjectMapper();

        mockServer.expect(requestTo(usersUrlFindUsersBySlackNames))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(MockRestRequestMatchers.content().string(String.format("{\"slackNames\":%s}", mapper.writeValueAsString(slackNames))))
                .andRespond(withSuccess(mapper.writeValueAsString(users), MediaType.APPLICATION_JSON_UTF8));
    }

    private void mockSuccessUsersServiceFindUsersByUuids(List<User> users) throws JsonProcessingException {
        List<String> uuids = new ArrayList<>();
        for (User user : users) {
            uuids.add(user.getUuid());
        }
        ObjectMapper mapper = new ObjectMapper();

        mockServer.expect(requestTo(usersUrlFindUsersByUuids))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(request -> assertThatJson(request.getBody().toString()).when(Option.IGNORING_ARRAY_ORDER)
                        .isEqualTo(String.format("{\"uuids\":%s}", mapper.writeValueAsString(uuids))))
                .andRespond(withSuccess(mapper.writeValueAsString(users), MediaType.APPLICATION_JSON_UTF8));
    }

    private void mockFailUsersServiceFindUsersByUuidsReturnsWrongUsersNumber(List<User> users) throws
            JsonProcessingException {
        List<String> uuids = new ArrayList<>();
        for (User user : users) {
            uuids.add(user.getUuid());
        }
        users.add(user5);
        ObjectMapper mapper = new ObjectMapper();

        mockServer.expect(requestTo(usersUrlFindUsersByUuids))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(request -> assertThatJson(request.getBody().toString()).when(Option.IGNORING_ARRAY_ORDER)
                        .isEqualTo(String.format("{\"uuids\":%s}", mapper.writeValueAsString(uuids))))
                .andRespond(withSuccess(mapper.writeValueAsString(users), MediaType.APPLICATION_JSON_UTF8));
    }

    private void mockSuccessTeamsServiceReturnsTeam(HttpMethod method, String expectedURI, String expectedRequestBody,
                                                    Team expectedTeam) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(method))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                .andExpect(request -> assertThatJson(request.getBody().toString()).when(Option.IGNORING_ARRAY_ORDER)
                        .isEqualTo(expectedRequestBody))
                .andRespond(withSuccess(mapper.writeValueAsString(expectedTeam), MediaType.APPLICATION_JSON_UTF8));
    }

    private void mockFailTeamsServiceReturnsTeamException(HttpMethod method, String expectedURI, String expectedRequestBody,
                                                          String teamException) {
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(method))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                .andExpect(request -> assertThatJson(request.getBody().toString())
                        .when(Option.IGNORING_ARRAY_ORDER)
                        .when(Option.IGNORING_EXTRA_FIELDS)
                        .isEqualTo(expectedRequestBody))
                .andRespond(withBadRequest().body(teamException));
    }

    private void mockSlackResponseUrl(String expectedURI, RichMessage delayedMessage) {
        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                .andExpect(request -> assertThatJson(request.getBody().toString())
                        .isEqualTo(mapper.writeValueAsString(delayedMessage)))
                .andRespond(withSuccess("OK", MediaType.APPLICATION_FORM_URLENCODED));
    }
}

