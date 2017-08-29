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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
public class TeamsSlackBotIntegrationTest {

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

    @Value("${teams.rest.api.version}")
    private String teamsRestApiVersion;
    @Value("${teams.baseURL}")
    private String teamsBaseUrl;
    @Value("${teams.endpoint.activateTeam}")
    private String teamsActivateTeamUrl;
    @Value("${teams.endpoint.deactivateTeam}")
    private String teamsDeactivateTeamUrl;
    @Value("${teams.endpoint.getTeam}")
    private String teamsGetTeamUrl;

    @Value("${users.rest.api.version}")
    private String usersRestApiVersion;
    @Value("${users.baseURL}")
    private String usersUrlBase;
    @Value("${users.endpoint.usersBySlackNames}")
    private String usersUrlFindUsersBySlackNames;
    @Value("${users.endpoint.usersByUuids}")
    private String usersUrlFindUsersByUuids;

    private String teamsSlackbotFullActivateTeamUrl;
    private String teamsSlackbotFullDeactivateTeamUrl;
    private String teamsSlackBotFullGetTeamUrl;
    private String teamsSlackBotFullGetMyTeamUrl;

    private String teamsFullActivateTeamUrl;
    private String teamsFullDeactivateTeamUrl;
    private String teamsFullGetTeamUrl;
    private String teamsFullGetMyTeamUrl;

    @Inject
    private RestTemplate restTemplate;

    @Inject
    private MockMvc mvc;
    private MockRestServiceServer mockServer;

    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @Before
    public void setup() {

        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        teamsSlackbotFullActivateTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotActivateTeamUrl;
        teamsSlackbotFullDeactivateTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotDeactivateTeamUrl;
        teamsSlackBotFullGetTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotGetTeamUrl;
        teamsSlackBotFullGetMyTeamUrl = "/" + teamsSlackbotRestApiVersion + teamsSlackbotCommandsUrl +
                teamsSlackbotGetMyTeamUrl;

        teamsFullActivateTeamUrl = teamsBaseUrl + "/" + teamsRestApiVersion + teamsActivateTeamUrl;
        teamsFullDeactivateTeamUrl = teamsBaseUrl + "/" + teamsRestApiVersion + teamsDeactivateTeamUrl;
        teamsFullGetTeamUrl = teamsBaseUrl + "/" + teamsRestApiVersion + teamsGetTeamUrl;

        user1 = new User("uuid1", "@slack1");
        user2 = new User("uuid2", "@slack2");
        user3 = new User("uuid3", "@slack3");
        user4 = new User("uuid4", "@slack4");
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = String.format("%s %s %s %s", user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack());
        final List<User> usersInCommand = Arrays.asList(new User[]{user1, user2, user3, user4});
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        String teamsJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        Set<String> uuids = new LinkedHashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid()));
        Team activatedTeam = new Team(uuids);
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.POST, teamsFullActivateTeamUrl, teamsJsonRequestBody,
                activatedTeam);
        mockSlackResponseUrl(HttpMethod.POST, responseUrl, new RichMessage(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE,
                commandText)));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullActivateTeamUrl),
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
        final List<User> usersInCommand = Arrays.asList(new User[]{user1, user2, user3, user4});
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        String teamsJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        Set<String> uuids = new LinkedHashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), "illegal-uuid"));
        Team activatedTeam = new Team(uuids);
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.POST, teamsFullActivateTeamUrl, teamsJsonRequestBody,
                activatedTeam);
        mockSlackResponseUrl(HttpMethod.POST, responseUrl,
                new RichMessage("Team members is not equals in request and response from Teams Service"));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullActivateTeamUrl),
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
        final List<User> usersInCommand = Arrays.asList(new User[]{user1, user2, user3, user4});
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        String teamsJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        String teamsJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryActivateTeamIfUsersInActiveTeamThrowsException.json"));
        mockFailTeamsServiceReturnsUserInTeamTeamException(HttpMethod.POST, teamsFullActivateTeamUrl, teamsJsonRequestBody,
                teamsJsonResponseBody);
        mockSuccessUsersServiceFindUsersByUuids(usersInCommand);
        mockSlackResponseUrl(HttpMethod.POST, responseUrl,
                new RichMessage(String.format("User(s) '%s' exist(s) in another teams",
                        usersInCommand.stream().
                                map(user -> user.getSlack())
                                .collect(Collectors.joining(",")))));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenThreeSlackNamesInTextShouldReturnErrorMessage() throws
            Exception {
        final String commandText = String.format("%s %s %s ", user1.getSlack(), user2.getSlack(), user3.getSlack());
        final List<User> usersInCommand = Arrays.asList(new User[]{user1, user2, user3});
        final int TEAM_SIZE = 4;
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        mockSlackResponseUrl(HttpMethod.POST, responseUrl, new RichMessage(
                String.format("We found %d slack names in your command." +
                        " But size of the team must be %s.", usersInCommand.size(), TEAM_SIZE)));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenSlackNamesNotExistsInUsersServiceShouldReturnErrorMessage()
            throws Exception {
        final String commandText = String.format("%s %s %s", user1.getSlack(), user2.getSlack(), user3.getSlack());
        final List<User> usersInCommand = Arrays.asList(new User[]{user1, user2, user3});
        String responseUrl = "http://example.com";
        mockFailUsersServiceFindUsersBySlackNames(usersInCommand);
        mockSlackResponseUrl(HttpMethod.POST, responseUrl, new RichMessage("Oops something went wrong :("));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullActivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        final String commandText = String.format("%s", user1.getSlack());
        final List<User> usersInCommand = Arrays.asList(new User[]{user1});
        String responseUrl = "http://example.com";
        mockSuccessUsersServiceFindUsersBySlackNames(usersInCommand);
        List<User> users = Arrays.asList(user1, user2, user3, user4);
        Team deactivatedTeam = new Team(users.stream().map(user -> user.getUuid()).collect(Collectors.toSet()));
        mockSuccessTeamsServiceReturnsTeam(HttpMethod.PUT, teamsFullDeactivateTeamUrl+"/"+user1.getUuid(), "",
                deactivatedTeam);
        mockSuccessUsersServiceFindUsersByUuids(users);
        mockSlackResponseUrl(HttpMethod.POST, responseUrl, new RichMessage(
                String.format(DEACTIVATE_TEAM_DELAYED_MESSAGE,
                        users.stream().map(user -> user.getSlack()).collect(Collectors.joining(" ")))));

        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(teamsSlackbotFullDeactivateTeamUrl),
                SlackUrlUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE,commandText)));
    }


    private void mockFailUsersServiceFindUsersBySlackNames(List<User> users) throws JsonProcessingException {
        List<String> slackNames = new ArrayList<>();
        for (User user : users) {
            slackNames.add(user.getSlack());
        }
        String userServiceURL = usersUrlBase + usersRestApiVersion + usersUrlFindUsersBySlackNames;

        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(userServiceURL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(MockRestRequestMatchers.content().string(String.format("{\"slackNames\":%s}", mapper.writeValueAsString(slackNames))))
                .andRespond(withBadRequest().body("{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}"));

    }

    private void mockSuccessUsersServiceFindUsersBySlackNames(List<User> users) throws JsonProcessingException {
        List<String> slackNames = new ArrayList<>();
        for (User user : users) {
            slackNames.add(user.getSlack());
        }
        String userServiceURL = usersUrlBase + usersRestApiVersion + usersUrlFindUsersBySlackNames;
        ObjectMapper mapper = new ObjectMapper();

        mockServer.expect(requestTo(userServiceURL))
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
        String userServiceURL = usersUrlBase + usersRestApiVersion + usersUrlFindUsersByUuids;
        ObjectMapper mapper = new ObjectMapper();

        mockServer.expect(requestTo(userServiceURL))
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

    private void mockFailTeamsServiceReturnsUserInTeamTeamException(HttpMethod method, String expectedURI, String expectedRequestBody,
                                                                    String teamException) throws IOException {
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(method))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                //.andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andExpect(request -> assertThatJson(request.getBody().toString()).when(Option.IGNORING_ARRAY_ORDER)
                        .when(Option.IGNORING_EXTRA_FIELDS)
                        .isEqualTo(expectedRequestBody))
                .andRespond(withBadRequest().body(teamException));
    }

    private void mockSlackResponseUrl(HttpMethod method, String expectedURI, RichMessage delayedMessage) {
        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(method))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                .andExpect(request -> assertThatJson(request.getBody().toString())
                        .isEqualTo(mapper.writeValueAsString(delayedMessage)))
                .andRespond(withSuccess("OK", MediaType.APPLICATION_FORM_URLENCODED));
    }
}

