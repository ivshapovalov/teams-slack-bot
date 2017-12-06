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
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackRequest;
import ua.com.juja.microservices.teams.slackbot.model.users.UserUuidRequest;
import ua.com.juja.microservices.teams.slackbot.repository.feign.TeamsClient;
import ua.com.juja.microservices.teams.slackbot.repository.feign.UsersClient;
import ua.com.juja.microservices.teams.slackbot.util.SlackUserHandler;
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
@SpringBootTest(classes = {TeamSlackBotApplication.class})
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

    private String teamsSlackbotActivateTeamUrl = "/v1/commands/teams/activate";
    private String teamsSlackbotDeactivateTeamUrl = "/v1/commands/teams/deactivate";
    private String teamsSlackbotGetTeamUrl = "/v1/commands/teams";
    private String teamsSlackbotGetMyTeamUrl = "/v1/commands/myteam";

    @Inject
    private RestTemplate restTemplate;
    @Inject
    private MockMvc mvc;
    private MockRestServiceServer mockServer;
    @MockBean
    private TeamsClient teamsClient;
    @MockBean
    private UsersClient usersClient;

    @BeforeClass
    public static void oneTimeSetUp() {
        user1 = new User("uuid1", "slack1");
        user2 = new User("uuid2", "slack2");
        user3 = new User("uuid3", "slack3");
        user4 = new User("uuid4", "slack4");
        userFrom = new User("uuid-from", "slack-from");
    }

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void onReceiveAllSlashCommandsWhenTokenIsIncorrectShouldReturnErrorMessage() throws Exception {
        String commandText = SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser());
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
        String commandText = SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser());
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
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(userFrom.getSlackUser()));
        List<User> usersInCommand = Arrays.asList(user1, user2, user3, userFrom);
        String responseUrl = "http://example.com";
        Set<String> slackUsersInCommand = usersInCommand.stream().map(User::getSlackUser).collect(Collectors.toSet());
        List<String> uuids = usersInCommand.stream().map(User::getUuid).collect(Collectors.toList());
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest = ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(usersInCommand);
        Set<String> members = new HashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(),
                userFrom.getUuid()));
        Team activatedTeam = new Team(members, userFrom.getUuid(), "id", new Date(), new Date());
        ArgumentCaptor<ActivateTeamRequest> captorActivateTeamRequest = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        when(teamsClient.activateTeam(captorActivateTeamRequest.capture())).thenReturn(activatedTeam);
        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest
                .class);
        when(usersClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInCommand);
        mockSlackResponseUrl(responseUrl, new RichMessage(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE,
                usersInCommand.stream()
                        .map(user -> SlackUserHandler.wrapSlackUserInFullPattern(user.getSlackUser()))
                        .sorted()
                        .collect(Collectors.joining(" ")))));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        mockServer.verify();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackRequest' slackUsers not contains 'slackUsersInCommand'")
                    .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
            soft.assertThat(captorActivateTeamRequest.getValue().getFrom())
                    .as("'captorActivateTeamRequest' from is not 'from-uuid'")
                    .isEqualTo(userFrom.getUuid());
            soft.assertThat(captorActivateTeamRequest.getValue().getMembers())
                    .as("'captorActivateTeamRequest' uuids not contains 'members'")
                    .containsExactlyInAnyOrder(members.toArray(new String[members.size()]));
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));
        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).activateTeam(captorActivateTeamRequest.capture());
        verify(usersClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenFromUserNotInTextShouldReturnOkMessage() throws Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user4.getSlackUser()));
        List<User> usersInCommand = Arrays.asList(user1, user2, user3, user4, userFrom);
        String responseUrl = "http://example.com";
        Set<String> slackUsersInCommand = usersInCommand.stream().map(User::getSlackUser).collect(Collectors.toSet());
        List<String> teamMembersUuids = Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(usersInCommand);
        Set<String> members = new HashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(),
                user4.getUuid()));
        Team activatedTeam = new Team(members, userFrom.getUuid(), "id", new Date(), new Date());
        ArgumentCaptor<ActivateTeamRequest> captorActivateTeamRequest = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        when(teamsClient.activateTeam(captorActivateTeamRequest.capture())).thenReturn(activatedTeam);
        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest
                .class);
        when(usersClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInCommand);
        mockSlackResponseUrl(responseUrl, new RichMessage(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE,
                usersInCommand.stream()
                        .map(user -> SlackUserHandler.wrapSlackUserInFullPattern(user.getSlackUser()))
                        .sorted()
                        .collect(Collectors.joining(" ")))));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        mockServer.verify();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackRequest' slackUsersInCommand not contains 'slackUsersInCommand'")
                    .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
            soft.assertThat(captorActivateTeamRequest.getValue().getFrom())
                    .as("'captorActivateTeamRequest' from is not 'from-uuid'")
                    .isEqualTo(userFrom.getUuid());
            soft.assertThat(captorActivateTeamRequest.getValue().getMembers())
                    .as("'captorActivateTeamRequest' teamMembersUuids not contains 'members'")
                    .containsExactlyInAnyOrder(members.toArray(new String[members.size()]));
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'teamMembersUuids'")
                    .containsExactlyInAnyOrder(teamMembersUuids.toArray(new String[teamMembersUuids.size()]));
        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).activateTeam(captorActivateTeamRequest.capture());
        verify(usersClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void
    onReceiveSlashCommandActivateTeamWhenRequestAndResponseOfTeamsServiceContainsDifferentUuidsShouldReturnErrorMessage()
            throws Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user4.getSlackUser()));
        List<User> usersInCommand = Arrays.asList(user1, user2, user3, user4, userFrom);
        String responseUrl = "http://example.com";
        List<String> slackUsersInCommand = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(),
                user4.getSlackUser(), userFrom.getSlackUser());
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(usersInCommand);
        Set<String> requestMembers = new HashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(),
                user4.getUuid()));
        Set<String> responseMembers = new HashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(),
                "illegal-uuid"));
        Team activatedTeam = new Team(responseMembers, userFrom.getUuid(), "id", new Date(), new Date());
        ArgumentCaptor<ActivateTeamRequest> captorActivateTeamRequest = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        when(teamsClient.activateTeam(captorActivateTeamRequest.capture())).thenReturn(activatedTeam);
        mockSlackResponseUrl(responseUrl,
                new RichMessage("Team members is not equals in request and response from Teams Service"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        mockServer.verify();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackRequest' slackUsers not contains 'slackUsersInCommand'")
                    .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
            soft.assertThat(captorActivateTeamRequest.getValue().getFrom())
                    .as("'captorActivateTeamRequest' from is not 'from-uuid'")
                    .isEqualTo(userFrom.getUuid());
            soft.assertThat(captorActivateTeamRequest.getValue().getMembers())
                    .as("'captorActivateTeamRequest' uuids not contains 'requestMembers'")
                    .containsExactlyInAnyOrder(requestMembers.toArray(new String[requestMembers.size()]));
        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).activateTeam(captorActivateTeamRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user4.getSlackUser()));
        String responseUrl = "http://example.com";
        List<String> slackUsersInCommand = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(),
                user4.getSlackUser(), userFrom.getSlackUser());
        List<String> teamMemberUuids = Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        Set<String> members = new HashSet<>(teamMemberUuids);
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        List<User> users = Arrays.asList(user1, user2, user3, user4, userFrom);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(users);
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
        when(teamsClient.activateTeam(captorActivateTeamRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        List<User> usersInMessage = Arrays.asList(user1, user2, user3, user4);
        when(usersClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInMessage);

        mockSlackResponseUrl(responseUrl,
                new RichMessage(String.format("User(s) '%s' exist(s) in another teams",
                        usersInMessage.stream().
                                map(user -> SlackUserHandler.wrapSlackUserInFullPattern(user.getSlackUser()))
                                .collect(Collectors.joining(",")))));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        mockServer.verify();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackRequest' slackUsers not contains 'slackUsersInCommand'")
                    .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
            soft.assertThat(captorActivateTeamRequest.getValue().getFrom())
                    .as("'captorActivateTeamRequest from' is not 'from-uuid'")
                    .isEqualTo(userFrom.getUuid());
            soft.assertThat(captorActivateTeamRequest.getValue().getMembers())
                    .as("'captorActivateTeamRequest' uuids not contains 'members'")
                    .containsExactlyInAnyOrder(members.toArray(new String[members.size()]));
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(teamMemberUuids.toArray(new String[teamMemberUuids.size()]));
        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).activateTeam(captorActivateTeamRequest.capture());
        verify(usersClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void
    onReceiveSlashCommandActivateTeamWhenTeamsServiceReturnErrorAndUserServiceReturnErrorShouldReturnErrorMessage()
            throws Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user4.getSlackUser()));
        String responseUrl = "http://example.com";
        List<String> slackUsersInCommand = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(),
                user4.getSlackUser(), userFrom.getSlackUser());
        List<String> teamMemberUuids = Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        Set<String> members = new HashSet<>(teamMemberUuids);
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        List<User> users = Arrays.asList(user1, user2, user3, user4, userFrom);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(users);
        ArgumentCaptor<ActivateTeamRequest> captorActivateTeamRequest = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        String expectedJsonResponseBodyFromTeamsService =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F1-D3\",\n" +
                        "  \"clientMessage\": \"Sorry, but the user already exists in team!\",\n" +
                        "  \"developerMessage\": \"The reason of the exception is that user already in team\",\n" +
                        "  \"exceptionMessage\": \"User(s) '#uuid1,uuid2,uuid3,uuid4#' exist(s) in another teams\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";
        String expectedJsonResponseBodyFromUserService =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}";

        FeignException feignExceptionFromTeamsService = mock(FeignException.class);
        when(teamsClient.activateTeam(captorActivateTeamRequest.capture())).thenThrow(feignExceptionFromTeamsService);
        when(feignExceptionFromTeamsService.getMessage()).thenReturn(expectedJsonResponseBodyFromTeamsService);

        FeignException feignExceptionFromUsersService = mock(FeignException.class);
        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        when(usersClient.findUsersByUuids(captorUserUuidRequest.capture())).thenThrow(feignExceptionFromUsersService);
        when(feignExceptionFromUsersService.getMessage()).thenReturn(expectedJsonResponseBodyFromUserService);

        mockSlackResponseUrl(responseUrl, new RichMessage("Oops something went wrong :("));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        mockServer.verify();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackRequest' slackUsers not contains 'slackUsersInCommand'")
                    .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
            soft.assertThat(captorActivateTeamRequest.getValue().getFrom())
                    .as("'captorActivateTeamRequest from' is not 'from-uuid'")
                    .isEqualTo(userFrom.getUuid());
            soft.assertThat(captorActivateTeamRequest.getValue().getMembers())
                    .as("'captorActivateTeamRequest' uuids not contains 'members'")
                    .containsExactlyInAnyOrder(members.toArray(new String[members.size()]));
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(teamMemberUuids.toArray(new String[teamMemberUuids.size()]));
        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).activateTeam(captorActivateTeamRequest.capture());
        verify(usersClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenThreeSlackUsersInTextShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = String.format("%s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()));
        String responseUrl = "http://example.com";
        mockSlackResponseUrl(responseUrl,
                new RichMessage("We found 3 slack user in your command. But size of the team must be 4."));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        mockServer.verify();
        verifyZeroInteractions(teamsClient);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenUserServiceReturnErrorShouldReturnErrorMessage() throws Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user4.getSlackUser()));
        List<String> slackUsersInCommand = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(),
                user4.getSlackUser(), userFrom.getSlackUser());
        String responseUrl = "http://example.com";
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}";

        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        mockSlackResponseUrl(responseUrl, new RichMessage("very big and scare error"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        mockServer.verify();
        Assertions.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandActivateTeamWhenUserServiceReturnNotApiErrorShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = String.format("%s %s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user4.getSlackUser()));
        List<String> slackUsersInCommand = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(),
                user4.getSlackUser(), userFrom.getSlackUser());
        String responseUrl = "http://example.com";
        Exception exception = new RuntimeException("some exception");
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenThrow(exception);
        mockSlackResponseUrl(responseUrl, new RichMessage("some exception"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotActivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(ACTIVATE_TEAM_INSTANT_MESSAGE));

        //then
        mockServer.verify();
        Assertions.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String commandText = SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser());
        List<User> usersInCommand = Arrays.asList(user1, userFrom);
        String responseUrl = "http://example.com";
        List<String> slackUsersInCommand = Arrays.asList(user1.getSlackUser(), userFrom.getSlackUser());
        List<String> uuids = Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(usersInCommand);
        Team deactivatedTeam = new Team(new HashSet<>(uuids), userFrom.getUuid(), "id", new Date(), new Date());
        ArgumentCaptor<DeactivateTeamRequest> captorDeactivateTeamRequest =
                ArgumentCaptor.forClass(DeactivateTeamRequest.class);
        when(teamsClient.deactivateTeam(captorDeactivateTeamRequest.capture())).thenReturn(deactivatedTeam);

        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        List<User> usersInTeam = Arrays.asList(user1, user2, user3, user4);
        when(usersClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInTeam);

        mockSlackResponseUrl(responseUrl, new RichMessage(
                String.format(DEACTIVATE_TEAM_DELAYED_MESSAGE,
                        usersInTeam.stream()
                                .map(user -> SlackUserHandler.wrapSlackUserInFullPattern(user.getSlackUser()))
                                .sorted().collect(Collectors
                                .joining(" ")))));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        mockServer.verify();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackRequest' slackUsers not contains 'slackUsersInCommand'")
                    .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
            DeactivateTeamRequest expectedDeactivateTeamRequest = new DeactivateTeamRequest(userFrom.getUuid(), user1.getUuid());
            soft.assertThat(captorDeactivateTeamRequest.getValue())
                    .as("'captorDeactivateTeamRequest' is not equals 'expectedDeactivateTeamRequest'")
                    .isEqualTo(expectedDeactivateTeamRequest);
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));

        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).deactivateTeam(captorDeactivateTeamRequest.capture());
        verify(usersClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenMoreThanOneSlackUserInTextShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = String.format("%s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()));
        String responseUrl = "http://example.com";
        mockSlackResponseUrl(responseUrl,
                new RichMessage("We found 3 slack user in your command. But expect one slack user."));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotDeactivateTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        mockServer.verify();
        verifyZeroInteractions(teamsClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser());
        List<User> usersInCommand = Arrays.asList(user1, userFrom);
        String responseUrl = "http://example.com";
        List<String> slackUsersInCommand = Arrays.asList(user1.getSlackUser(), userFrom.getSlackUser());
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(usersInCommand);
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
        when(teamsClient.deactivateTeam(captorDeactivateTeamRequest.capture())).thenThrow(feignException);
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
        mockServer.verify();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackRequest' slackUsers not contains 'slackUsersInCommand'")
                    .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
            DeactivateTeamRequest expectedDeactivateTeamRequest =
                    new DeactivateTeamRequest(userFrom.getUuid(), user1.getUuid());
            soft.assertThat(captorDeactivateTeamRequest.getValue())
                    .as("'captorDeactivateTeamRequest' is not equals 'expectedDeactivateTeamRequest'")
                    .isEqualTo(expectedDeactivateTeamRequest);
        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).deactivateTeam(captorDeactivateTeamRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String from = SlackUserHandler.wrapSlackUserInFullPattern(userFrom.getSlackUser());
        List<User> usersInCommand = Collections.singletonList(userFrom);
        String responseUrl = "http://example.com";
        List<String> slackUsersInCommand = Collections.singletonList(userFrom.getSlackUser());
        List<String> uuids = Arrays.asList(userFrom.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        List<String> teamMembersSlackUsers = Arrays.asList(userFrom.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(),
                user4.getSlackUser());
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(usersInCommand);
        Team myTeam = new Team(new HashSet<>(uuids), userFrom.getUuid(), "id", new Date(), new Date());
        when(teamsClient.getTeam(userFrom.getUuid())).thenReturn(myTeam);

        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        List<User> usersInTeam = Arrays.asList(userFrom, user2, user3, user4);
        when(usersClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInTeam);

        mockSlackResponseUrl(responseUrl,
                new RichMessage(String.format(GET_MY_TEAM_DELAYED_MESSAGE, from,
                        teamMembersSlackUsers.stream()
                                .sorted()
                                .map(SlackUserHandler::wrapSlackUserInFullPattern)
                                .collect(Collectors.joining(" ")))));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", "", responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, from)));

        //then
        mockServer.verify();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackRequest' slackUsers not contains 'slackUsersInCommand'")
                    .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));

        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).getTeam(userFrom.getUuid());
        verify(usersClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenUserServiceReturnErrorShouldReturnErrorMessage()
            throws Exception {
        //given
        String from = SlackUserHandler.wrapSlackUserInFullPattern(userFrom.getSlackUser());
        List<String> slackUsersInCommand = Collections.singletonList(userFrom.getSlackUser());
        String responseUrl = "http://example.com";
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}";

        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);

        mockSlackResponseUrl(responseUrl, new RichMessage("very big and scare error"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetMyTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", from, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_MY_TEAM_INSTANT_MESSAGE, from)));

        //then
        mockServer.verify();
        Assertions.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        //given
        String from = SlackUserHandler.wrapSlackUserInFullPattern(userFrom.getSlackUser());
        List<User> usersInCommand = Collections.singletonList(userFrom);
        List<String> slackUsersInCommand = Collections.singletonList(userFrom.getSlackUser());
        String responseUrl = "http://example.com";
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(usersInCommand);
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
        when(teamsClient.getTeam(userFrom.getUuid())).thenThrow(feignException);
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
        mockServer.verify();
        Assertions.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).getTeam(userFrom.getUuid());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenAllCorrectShouldReturnOkMessage() throws Exception {
        //given
        String commandText = SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()) + " some test";
        List<User> usersInCommand = Collections.singletonList(user1);
        List<String> slackUsersInCommand = Collections.singletonList(user1.getSlackUser());
        String responseUrl = "http://example.com";
        List<String> uuids = Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
        List<String> teamMembersSlackUsers = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(),
                user3.getSlackUser(), user4.getSlackUser());
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(usersInCommand);
        Team usersTeam = new Team(new HashSet<>(uuids), userFrom.getUuid(), "id", new Date(), new Date());
        when(teamsClient.getTeam(user1.getUuid())).thenReturn(usersTeam);

        ArgumentCaptor<UserUuidRequest> captorUserUuidRequest = ArgumentCaptor.forClass(UserUuidRequest.class);
        List<User> usersInTeam = Arrays.asList(user1, user2, user3, user4);
        when(usersClient.findUsersByUuids(captorUserUuidRequest.capture())).thenReturn(usersInTeam);

        mockSlackResponseUrl(responseUrl,
                new RichMessage(String.format(GET_TEAM_DELAYED_MESSAGE,
                        SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                        teamMembersSlackUsers.stream()
                                .sorted()
                                .map(SlackUserHandler::wrapSlackUserInFullPattern)
                                .collect(Collectors.joining(" ")))));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        mockServer.verify();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                    .as("'captorUserSlackRequest' slackUsers not contains 'slackUsersInCommand'")
                    .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
            soft.assertThat(captorUserUuidRequest.getValue().getUuids())
                    .as("'captorUserUuidRequest' uuids not contains 'uuids'")
                    .containsExactlyInAnyOrder(uuids.toArray(new String[uuids.size()]));

        });
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).getTeam(user1.getUuid());
        verify(usersClient).findUsersByUuids(captorUserUuidRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenMoreThanOneSlackUserInTextShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = String.format("%s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()));
        String responseUrl = "http://example.com";
        mockSlackResponseUrl(responseUrl,
                new RichMessage("We found 3 slack user in your command. But expect one slack user."));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        mockServer.verify();
        verifyZeroInteractions(teamsClient);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenUserServiceReturnErrorShouldReturnErrorMessage()
            throws Exception {
        //given
        String commandText = SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser());
        List<String> slackUsersInCommand = Collections.singletonList(user1.getSlackUser());
        String responseUrl = "http://example.com";
        String expectedJsonResponseBody =
                "status 400 reading GatewayClient#activateTeam(ActivateTeamRequest); content:" +
                        "{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}";

        FeignException feignException = mock(FeignException.class);
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        mockSlackResponseUrl(responseUrl, new RichMessage("very big and scare error"));

        //when
        mvc.perform(MockMvcRequestBuilders.post(TestUtils.getUrlTemplate(teamsSlackbotGetTeamUrl),
                TestUtils.getUriVars("slashCommandToken", "/command", commandText, responseUrl))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format(GET_TEAM_INSTANT_MESSAGE, commandText)));

        //then
        mockServer.verify();
        Assertions.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verifyNoMoreInteractions(teamsClient, usersClient);
    }

    @Test
    public void onReceiveSlashCommandGetTeamWhenTeamsServiceReturnErrorShouldReturnErrorMessage() throws
            Exception {
        //given
        String commandText = SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser());
        List<User> usersInCommand = Collections.singletonList(user1);
        String responseUrl = "http://example.com";
        List<String> slackUsersInCommand = Collections.singletonList(user1.getSlackUser());
        ArgumentCaptor<UserSlackRequest> captorUserSlackRequest =
                ArgumentCaptor.forClass(UserSlackRequest.class);
        when(usersClient.findUsersBySlackUsers(captorUserSlackRequest.capture())).thenReturn(usersInCommand);
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
        when(teamsClient.getTeam(user1.getUuid())).thenThrow(feignException);
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
        Assertions.assertThat(captorUserSlackRequest.getValue().getSlackUsers())
                .containsExactlyInAnyOrder(slackUsersInCommand.toArray(new String[slackUsersInCommand.size()]));
        verify(usersClient).findUsersBySlackUsers(captorUserSlackRequest.capture());
        verify(teamsClient).getTeam(user1.getUuid());
        verifyNoMoreInteractions(teamsClient, usersClient);
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
