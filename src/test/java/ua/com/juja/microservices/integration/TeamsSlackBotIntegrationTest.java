package ua.com.juja.microservices.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.TeamSlackBotApplication;
import ua.com.juja.microservices.teams.slackbot.model.teams.ActivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.DeactivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.Team;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.users.UserUuidRequest;
import ua.com.juja.microservices.teams.slackbot.repository.impl.GatewayClient;
import ua.com.juja.microservices.utils.TestUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TeamSlackBotApplication.class}, properties = {"eureka.client.enabled=false"})
@AutoConfigureMockMvc
@TestPropertySource(value = {"classpath:application.properties", "classpath:messages/message.properties"})
public class TeamsSlackBotIntegrationTest {

    private static User user1;
    private static User user2;
    private static User user3;
    private static User user4;
    private static User userFrom;

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

    @MockBean
    private GatewayClient gatewayClient;

    @BeforeClass
    public static void oneTimeSetUp() {
        user1 = new User("uuid1", "@slack1");
        user2 = new User("uuid2", "@slack2");
        user3 = new User("uuid3", "@slack3");
        user4 = new User("uuid4", "@slack4");
        userFrom = new User("uuid-from", "@slack-from");
    }

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void onReceiveAllSlashCommandsWhenTokenIsIncorrectShouldReturnErrorMessage() throws Exception {
        String commandText = user1.getSlack();
        String responseUrl = "http:/example.com";
        List<String> urls = Arrays.asList(
                teamsSlackbotActivateTeamUrl,
                teamsSlackbotDeactivateTeamUrl,
                teamsSlackbotGetTeamUrl,
                teamsSlackbotGetMyTeamUrl);
        urls.forEach(url -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(url),
                        TestUtils.getUriVars("wrongToken", "/command", commandText, responseUrl))
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
        String commandText = user1.getSlack();
        String responseUrl = "";
        List<String> urls = Arrays.asList(
                teamsSlackbotActivateTeamUrl,
                teamsSlackbotDeactivateTeamUrl,
                teamsSlackbotGetTeamUrl,
                teamsSlackbotGetMyTeamUrl);
        urls.forEach(url -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(url),
                        TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .andExpect(status().isOk())
                        .andExpect(content().string(SORRY_MESSAGE));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenFromUserInTextShouldReturnOkMessage() throws Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                user1.getSlack(), user2.getSlack(), user3.getSlack(), userFrom.getSlack());
        List<User> usersInText = Arrays.asList(user1, user2, user3, userFrom);
        String responseUrl = "http://example.com";
        List<String> slackNames = Arrays.asList(user1.getSlack(), user2.getSlack(), user3.getSlack(), userFrom.getSlack());
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest = ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(usersInText);
        Set<String> members = new HashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(),
                userFrom.getUuid()));
        Team activatedTeam = new Team(members, userFrom.getUuid(), "id", new Date(), new Date());
        ArgumentCaptor<ActivateTeamRequest> captorActivateTeamRequest = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        when(gatewayClient.activateTeam(captorActivateTeamRequest.capture())).thenReturn(activatedTeam);
        mockSlackResponseUrl(responseUrl, new RichMessage(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE, commandText)));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .as("'captorUserSlackNameRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorActivateTeamRequest.getValue().getFrom())
                    .as("'captorActivateTeamRequest' from is not 'from-uuid'")
                    .isEqualTo(userFrom.getUuid());
            soft.assertThat(captorActivateTeamRequest.getValue().getMembers())
                    .as("'captorActivateTeamRequest' uuids not contains 'members'")
                    .containsExactlyInAnyOrder(members.toArray(new String[members.size()]));
        });
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).activateTeam(captorActivateTeamRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenFromUserNotInTextShouldReturnOkMessage() throws Exception {
        //given
        String commandText = String.format("%s %s %s %s", user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack());
        List<User> usersInText = Arrays.asList(user1, user2, user3, user4, userFrom);
        String responseUrl = "http://example.com";
        List<String> slackNames = Arrays.asList(user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack(), userFrom.getSlack());
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest = ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(usersInText);
        Set<String> members = new HashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(),
                user4.getUuid()));
        Team activatedTeam = new Team(members, userFrom.getUuid(), "id", new Date(), new Date());
        ArgumentCaptor<ActivateTeamRequest> captorActivateTeamRequest = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        when(gatewayClient.activateTeam(captorActivateTeamRequest.capture())).thenReturn(activatedTeam);
        mockSlackResponseUrl(responseUrl, new RichMessage(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE, commandText)));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .as("'captorUserSlackNameRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorActivateTeamRequest.getValue().getFrom())
                    .as("'captorActivateTeamRequest' from is not 'from-uuid'")
                    .isEqualTo(userFrom.getUuid());
            soft.assertThat(captorActivateTeamRequest.getValue().getMembers())
                    .as("'captorActivateTeamRequest' uuids not contains 'members'")
                    .containsExactlyInAnyOrder(members.toArray(new String[members.size()]));
        });
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).activateTeam(captorActivateTeamRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void
    onReceiveSlashCommandActivateTeamWhenRequestAndResponseOfTeamsServiceContainsDifferentUuidsShouldReturnErrorMessage()
            throws Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                user1.getSlack(), user2.getSlack(), user3.getSlack(), user4.getSlack());
        List<User> users = Arrays.asList(user1, user2, user3, user4, userFrom);
        String responseUrl = "http://example.com";
        List<String> slackNames = Arrays.asList(user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack(), userFrom.getSlack());
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest = ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(users);
        Set<String> requestMembers = new HashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(),
                user4.getUuid()));
        Set<String> responseMembers = new HashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(),
                "illegal-uuid"));
        Team activatedTeam = new Team(responseMembers, userFrom.getUuid(), "id", new Date(), new Date());
        ArgumentCaptor<ActivateTeamRequest> captorActivateTeamRequest = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        when(gatewayClient.activateTeam(captorActivateTeamRequest.capture())).thenReturn(activatedTeam);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Team members is not equals in request and response from Teams Service"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .as("'captorUserSlackNameRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorActivateTeamRequest.getValue().getFrom())
                    .as("'captorActivateTeamRequest' from is not 'from-uuid'")
                    .isEqualTo(userFrom.getUuid());
            soft.assertThat(captorActivateTeamRequest.getValue().getMembers())
                    .as("'captorActivateTeamRequest' uuids not contains 'requestMembers'")
                    .containsExactlyInAnyOrder(requestMembers.toArray(new String[requestMembers.size()]));
        });
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).activateTeam(captorActivateTeamRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                user1.getSlack(), user2.getSlack(), user3.getSlack(), user4.getSlack());
        String responseUrl = "http://example.com";
        List<String> slackNames = Arrays.asList(user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack(), userFrom.getSlack());
        List<String> uuids = Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        Set<String> members = new HashSet<>(uuids);
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        List<User> users = Arrays.asList(user1, user2, user3, user4, userFrom);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(users);
        ArgumentCaptor<ActivateTeamRequest> captorActivateTeamRequest = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F1-D3\",\n" +
                        "  \"clientMessage\": \"Sorry, but the user already exists in team!\",\n" +
                        "  \"developerMessage\": \"The reason of the exception is that user already in team\",\n" +
                        "  \"exceptionMessage\": \"User(s) '#uuid1,uuid2,uuid3,uuid4#' exist(s) in another teams\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";

        FeignException feignException = mock(FeignException.class);
        when(gatewayClient.activateTeam(captorActivateTeamRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        List<User> usersInMessage = Arrays.asList(user1, user2, user3, user4);
        when(gatewayClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInMessage);

        mockSlackResponseUrl(responseUrl,
                new RichMessage(String.format("User(s) '%s' exist(s) in another teams",
                        usersInMessage.stream().
                                map(User::getSlack)
                                .collect(Collectors.joining(",")))));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .as("'captorUserSlackNameRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorActivateTeamRequest.getValue().getFrom())
                    .as("'captorActivateTeamRequest from' is not 'from-uuid'")
                    .isEqualTo(userFrom.getUuid());
            soft.assertThat(captorActivateTeamRequest.getValue().getMembers())
                    .as("'captorActivateTeamRequest' uuids not contains 'members'")
                    .containsExactlyInAnyOrder(members.toArray(new String[members.size()]));
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));
        });
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).activateTeam(captorActivateTeamRequest.capture());
        verify(gatewayClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenThreeSlackNamesInTextShouldReturnErrorMessage() throws
            Exception {
        String commandText = String.format("%s %s %s ", user1.getSlack(), user2.getSlack(), user3.getSlack());
        String responseUrl = "http://example.com";
        mockSlackResponseUrl(responseUrl,
                new RichMessage("We found 3 slack names in your command. But size of the team must be 4."));

        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        verifyZeroInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenUserServiceReturnErrorShouldReturnErrorMessage() throws Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                user1.getSlack(), user2.getSlack(), user3.getSlack(), user4.getSlack());
        List<String> slackNames = Arrays.asList(user1.getSlack(), user2.getSlack(), user3.getSlack(),
                user4.getSlack(), userFrom.getSlack());
        String responseUrl = "http://example.com";
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}";

        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        mockSlackResponseUrl(responseUrl, new RichMessage("very big and scare error"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        Assertions.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String commandText = user1.getSlack();
        List<User> usersInCommand = Arrays.asList(user1, userFrom);
        String responseUrl = "http://example.com";
        List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        List<String> uuids = Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(usersInCommand);
        Team deactivatedTeam = new Team(new HashSet<>(uuids), userFrom.getUuid(), "id", new Date(), new Date());
        ArgumentCaptor<DeactivateTeamRequest> captorDeactivateTeamRequest =
                ArgumentCaptor.forClass(DeactivateTeamRequest.class);
        when(gatewayClient.deactivateTeam(captorDeactivateTeamRequest.capture())).thenReturn(deactivatedTeam);

        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        List<User> usersInTeam = Arrays.asList(user1, user2, user3, user4);
        when(gatewayClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInTeam);

        mockSlackResponseUrl(responseUrl, new RichMessage(
                String.format(DEACTIVATE_TEAM_DELAYED_MESSAGE,
                        usersInTeam.stream().map(User::getSlack).collect(Collectors.joining(" ")))));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .as("'captorUserSlackNameRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            DeactivateTeamRequest expectedDeactivateTeamRequest = new DeactivateTeamRequest(userFrom.getUuid(), user1.getUuid());
            soft.assertThat(captorDeactivateTeamRequest.getValue())
                    .as("'captorDeactivateTeamRequest' is not equals 'expectedDeactivateTeamRequest'")
                    .isEqualTo(expectedDeactivateTeamRequest);
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));

        });
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).deactivateTeam(captorDeactivateTeamRequest.capture());
        verify(gatewayClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenMoreThanOneSlackNameInTextShouldReturnErrorMessage() throws
            Exception {
        String commandText = String.format("%s %s %s ", user1.getSlack(), user2.getSlack(), user3.getSlack());
        String responseUrl = "http://example.com";
        mockSlackResponseUrl(responseUrl,
                new RichMessage("We found 3 slack names in your command. But expect one slack name."));

        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));

        verifyZeroInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = user1.getSlack();
        List<User> usersInCommand = Arrays.asList(user1, userFrom);
        String responseUrl = "http://example.com";
        List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(usersInCommand);
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F2-D2\",\n" +
                        "  \"clientMessage\": \"You cannot get/deactivate team if the user not a member of any team!\",\n" +
                        "  \"developerMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"exceptionMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";
        ArgumentCaptor<DeactivateTeamRequest> captorDeactivateTeamRequest = ArgumentCaptor.forClass(DeactivateTeamRequest
                .class);
        FeignException feignException = mock(FeignException.class);
        when(gatewayClient.deactivateTeam(captorDeactivateTeamRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        mockSlackResponseUrl(responseUrl,
                new RichMessage("You cannot get/deactivate team if the user not a member of any team!"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .as("'captorUserSlackNameRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            DeactivateTeamRequest expectedDeactivateTeamRequest =
                    new DeactivateTeamRequest(userFrom.getUuid(), user1.getUuid());
            soft.assertThat(captorDeactivateTeamRequest.getValue())
                    .as("'captorDeactivateTeamRequest' is not equals 'expectedDeactivateTeamRequest'")
                    .isEqualTo(expectedDeactivateTeamRequest);
        });
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).deactivateTeam(captorDeactivateTeamRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String from = userFrom.getSlack();
        List<User> usersInCommand = Collections.singletonList(userFrom);
        String responseUrl = "http://example.com";
        List<String> slackNames = Collections.singletonList(userFrom.getSlack());
        List<String> uuids = Arrays.asList(userFrom.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(usersInCommand);
        Team myTeam = new Team(new HashSet<>(uuids), userFrom.getUuid(), "id", new Date(), new Date());
        when(gatewayClient.getTeam(userFrom.getUuid())).thenReturn(myTeam);

        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        List<User> usersInTeam = Arrays.asList(user1, user2, user3, user4);
        when(gatewayClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInTeam);

        mockSlackResponseUrl(responseUrl,
                new RichMessage(String.format(GET_MY_TEAM_DELAYED_MESSAGE, from, slackNames)));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", "", responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, from)));

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .as("'captorUserSlackNameRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));

        });
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).getTeam(userFrom.getUuid());
        verify(gatewayClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenUserServiceReturnErrorShouldReturnErrorMessage()
            throws Exception {
        //given
        String from = userFrom.getSlack();
        List<String> slackNames = Collections.singletonList(userFrom.getSlack());
        String responseUrl = "http://example.com";
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}";

        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        mockSlackResponseUrl(responseUrl, new RichMessage("very big and scare error"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", from, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, from)));

        //then
        Assertions.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        //given
        String from = userFrom.getSlack();
        List<User> usersInCommand = Collections.singletonList(userFrom);
        String responseUrl = "http://example.com";
        List<String> slackNames = Collections.singletonList(userFrom.getSlack());
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(usersInCommand);
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F2-D2\",\n" +
                        "  \"clientMessage\": \"You cannot get/deactivate team if the user not a member of any team!\",\n" +
                        "  \"developerMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"exceptionMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";
        FeignException feignException = mock(FeignException.class);
        when(gatewayClient.getTeam(userFrom.getUuid())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("You cannot get/deactivate team if the user not a member of any team!"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", from, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, from)));

        //then
        Assertions.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).getTeam(userFrom.getUuid());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String commandText = user1.getSlack();
        List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        List<String> slackNames = Collections.singletonList(user1.getSlack());
        List<String> uuids = Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(usersInCommand);
        Team usersTeam = new Team(new HashSet<>(uuids), userFrom.getUuid(), "id", new Date(), new Date());
        when(gatewayClient.getTeam(user1.getUuid())).thenReturn(usersTeam);

        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        List<User> usersInTeam = Arrays.asList(user1, user2, user3, user4);
        when(gatewayClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInTeam);

        mockSlackResponseUrl(responseUrl,
                new RichMessage(String.format(GET_TEAM_DELAYED_MESSAGE, commandText, slackNames)));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                    .as("'captorUserSlackNameRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));

        });
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).getTeam(user1.getUuid());
        verify(gatewayClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenMoreThanOneSlackNameInTextShouldReturnErrorMessage() throws
            Exception {
        String commandText = String.format("%s %s %s ", user1.getSlack(), user2.getSlack(), user3.getSlack());
        String responseUrl = "http://example.com";
        mockSlackResponseUrl(responseUrl,
                new RichMessage("We found 3 slack names in your command. But expect one slack name."));

        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));

        verifyZeroInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenUserServiceReturnErrorShouldReturnErrorMessage()
            throws Exception {
        //given
        String commandText = user1.getSlack();
        List<String> slackNames = Collections.singletonList(user1.getSlack());
        String responseUrl = "http://example.com";
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}";

        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        mockSlackResponseUrl(responseUrl, new RichMessage("very big and scare error"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        Assertions.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verifyNoMoreInteractions(gatewayClient);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = user1.getSlack();
        List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        List<String> slackNames = Collections.singletonList(user1.getSlack());
        ArgumentCaptor<UserSlackNameRequest> captorUserSlackNameRequest =
                ArgumentCaptor.forClass(UserSlackNameRequest.class);
        when(gatewayClient.findUsersBySlackNames(captorUserSlackNameRequest.capture())).thenReturn(usersInCommand);
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F2-D2\",\n" +
                        "  \"clientMessage\": \"You cannot get/deactivate team if the user not a member of any team!\",\n" +
                        "  \"developerMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"exceptionMessage\": \"The reason of the exception is that user not in team\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";

        FeignException feignException = mock(FeignException.class);
        when(gatewayClient.getTeam(user1.getUuid())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("You cannot get/deactivate team if the user not a member of any team!"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        Assertions.assertThat(captorUserSlackNameRequest.getValue().getSlackNames())
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
        verify(gatewayClient).findUsersBySlackNames(captorUserSlackNameRequest.capture());
        verify(gatewayClient).getTeam(user1.getUuid());
        verifyNoMoreInteractions(gatewayClient);
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
