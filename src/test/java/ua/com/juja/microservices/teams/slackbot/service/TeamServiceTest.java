package ua.com.juja.microservices.teams.slackbot.service;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;
import ua.com.juja.microservices.teams.slackbot.model.teams.ActivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.DeactivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.Team;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.repository.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.util.SlackUserHandler;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TeamServiceTest {
    private static final int TEAM_SIZE = 4;
    private static User user1;
    private static User user2;
    private static User user3;
    private static User user4;
    private static User userFrom;
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();
    @MockBean
    private TeamRepository teamRepository;
    @MockBean
    private UserService userService;
    @Inject
    private TeamService teamService;

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
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void activateTeamIfMembersSizeEqualsFourAndFromUserInTeamExecutedCorrectly() {
        //given
        String text = String.format("%s %s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(userFrom.getSlackUser()));
        Set<String> uuids = new LinkedHashSet<>(Arrays.asList(
                user1.getUuid(), user2.getUuid(), user3.getUuid(), userFrom.getUuid()));
        List<User> users = Arrays.asList(user1, user2, user3, userFrom);
        Team activatedTeam = new Team(uuids, userFrom.getUuid(), "id", new Date(), new Date());
        Set<String> expected = new LinkedHashSet<>(
                Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(), userFrom.getSlackUser()));

        when(userService.findUsersBySlackUsers(anyListOf(String.class))).thenReturn(users);
        given(teamRepository.activateTeam(any(ActivateTeamRequest.class))).willReturn(activatedTeam);
        when(userService.findUsersByUuids(anyListOf(String.class))).thenReturn(users);

        //when
        Set<String> actual = teamService.activateTeam(userFrom.getSlackUser(), text);

        //then
        assertEquals(expected, actual);
        verify(userService).findUsersBySlackUsers(anyListOf(String.class));
        ArgumentCaptor<ActivateTeamRequest> captor = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        verify(teamRepository).activateTeam(captor.capture());
        assertTrue(captor.getValue().getMembers().equals(uuids));
        assertTrue(captor.getValue().getFrom().equals(userFrom.getUuid()));
        verify(userService).findUsersByUuids(anyListOf(String.class));
        verifyNoMoreInteractions(userService, teamRepository);
    }

    @Test
    public void activateTeamIfMembersSizeEqualsFourAndFromUserNotInTeamExecutedCorrectly() {
        //given
        String text = String.format("%s %s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user4.getSlackUser()));
        Set<String> uuids = new LinkedHashSet<>(Arrays.asList(
                user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid()));
        List<User> users = Arrays.asList(user1, user2, user3, user4, userFrom);
        when(userService.findUsersBySlackUsers(anyListOf(String.class))).thenReturn(users);
        Set<String> expected = new LinkedHashSet<>(
                Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(), user4.getSlackUser()));
        Team activatedTeam = new Team(uuids, userFrom.getUuid(), "id", new Date(), new Date());
        List<User> usersInTeam = Arrays.asList(user1, user2, user3, user4);
        given(teamRepository.activateTeam(any(ActivateTeamRequest.class))).willReturn(activatedTeam);
        when(userService.findUsersByUuids(anyListOf(String.class))).thenReturn(usersInTeam);

        //when
        Set<String> actual = teamService.activateTeam(userFrom.getSlackUser(), text);

        //then
        assertEquals(expected, actual);
        verify(userService).findUsersBySlackUsers(anyListOf(String.class));
        ArgumentCaptor<ActivateTeamRequest> captor = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        verify(teamRepository).activateTeam(captor.capture());
        assertTrue(captor.getValue().getMembers().equals(uuids));
        assertTrue(captor.getValue().getFrom().equals(userFrom.getUuid()));
        verify(userService).findUsersByUuids(anyListOf(String.class));
        verifyNoMoreInteractions(userService, teamRepository);
    }

    @Test
    public void activateTeamIfMembersSizeNotEqualsFourThrowsException() {
        //given
        String from = userFrom.getSlackUser();
        String text = String.format("%s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()));

        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found 3 slack user in your command." +
                " But size of the team must be %s.", TEAM_SIZE));

        try {
            //when
            teamService.activateTeam(from, text);
        } finally {
            //then
            verifyZeroInteractions(userService, teamRepository);
        }
    }

    @Test
    public void activateTeamIfMembersOfRequestAndResponseOfTeamsServiceNotEqualsThrowsException() {
        //given
        String from = userFrom.getSlackUser();
        String text = String.format("%s %s %s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user3.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user4.getSlackUser()));
        List<String> slackUsers = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(), user4.getSlackUser(), from);
        Set<String> requestMembers = new LinkedHashSet<>(Arrays.asList(
                user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid()));
        Set<String> responseMembers = new LinkedHashSet<>(Arrays.asList(
                user1.getUuid(), user2.getUuid(), user3.getUuid(), "uuid5"));
        List<User> users = Arrays.asList(user1, user2, user3, user4, userFrom);
        when(userService.findUsersBySlackUsers(slackUsers)).thenReturn(users);
        Team activatedTeam = new Team(responseMembers, userFrom.getUuid(), "id", new Date(), new Date());
        given(teamRepository.activateTeam(any(ActivateTeamRequest.class))).willReturn(activatedTeam);

        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage("Team members is not equals in request and response from Teams Service");

        try {
            //when
            teamService.activateTeam(from, text);
        } finally {
            //then
            verify(userService).findUsersBySlackUsers(slackUsers);
            ArgumentCaptor<ActivateTeamRequest> captor = ArgumentCaptor.forClass(ActivateTeamRequest.class);
            verify(teamRepository).activateTeam(captor.capture());
            assertTrue(captor.getValue().getMembers().equals(requestMembers));
            assertTrue(captor.getValue().getFrom().equals(userFrom.getUuid()));
            verifyNoMoreInteractions(userService, teamRepository);
        }
    }

    @Test
    public void activateTeamIfTextIsNullThrowsException() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Text must not be null!");

        teamService.activateTeam("from-id", null);
    }

    @Test
    public void activateTeamIfFromUserIsNullThrowsException() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("FromSlackUser must not be null!");

        teamService.activateTeam(null, "text");
    }

    @Test
    public void getTeamIfOneSlackUserInTextExecutedCorrectly() {
        //given
        String text = String.format("%s", SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()));
        List<String> slackUsersInText = Collections.singletonList(user1.getSlackUser());
        List<User> users = Collections.singletonList(user1);
        List<User> teamUsers = Arrays.asList(user1, user2, user3, user4);
        Set<String> uuids = teamUsers.stream().map(User::getUuid).collect(Collectors.toSet());
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(),
                user4.getSlackUser()));
        Team team = new Team(uuids, userFrom.getUuid(), "id", new Date(), new Date());
        given(userService.findUsersBySlackUsers(slackUsersInText)).willReturn(users);
        given(teamRepository.getTeam(user1.getUuid())).willReturn(team);
        given(userService.findUsersByUuids(anyListOf(String.class))).willReturn(teamUsers);

        //when
        Set<String> actual = teamService.getTeam(text);

        //then
        assertEquals(expected, actual);
        verify(userService).findUsersBySlackUsers(slackUsersInText);
        verify(teamRepository).getTeam(user1.getUuid());
        verify(userService).findUsersByUuids(anyListOf(String.class));
        verifyNoMoreInteractions(teamRepository, userService);
    }

    @Test
    public void getTeamIfMoreThanOneSlackUserInTextThrowsException() {
        String text = String.format("%s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()));
        List<User> users = Arrays.asList(user1, user2);
        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found %d slack user in your command." +
                " But expect one slack user.", users.size()));

        teamService.getTeam(text);
    }

    @Test
    public void getTeamIfZeroSlackUserInTextThrowsException() {
        String text = "slack1 slack2";
        List<String> slackUsersInText = Collections.emptyList();
        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found %d slack user in your command." +
                " But expect one slack user.", slackUsersInText.size()));

        teamService.getTeam(text);
    }

    @Test
    public void deactivateTeamIfOneFromUserSlackUserInTextExecutedCorrectly() {
        //given
        String from = userFrom.getSlackUser();
        String text = String.format("%s", SlackUserHandler.wrapSlackUserInFullPattern(userFrom.getSlackUser()));
        List<String> slackUsers = Collections.singletonList(userFrom.getSlackUser());
        List<User> users = Collections.singletonList(userFrom);
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(
                user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(), user4.getSlackUser()));
        given(userService.findUsersBySlackUsers(slackUsers)).willReturn(users);
        List<User> teamUsers = Arrays.asList(user1, user2, user3, user4);
        given(userService.findUsersByUuids(anyListOf(String.class))).willReturn(teamUsers);
        Team team = new Team(teamUsers.stream().map(User::getUuid).collect(Collectors.toSet()), userFrom.getUuid(),
                "id", new Date(), new Date());
        given(teamRepository.deactivateTeam(any(DeactivateTeamRequest.class))).willReturn(team);

        //when
        Set<String> actual = teamService.deactivateTeam(from, text);

        //then
        assertThat(actual, is(expected));
        verify(userService).findUsersBySlackUsers(slackUsers);
        ArgumentCaptor<DeactivateTeamRequest> captor = ArgumentCaptor.forClass(DeactivateTeamRequest.class);
        verify(teamRepository).deactivateTeam(captor.capture());
        assertTrue(captor.getValue().getFrom().equals(userFrom.getUuid()));
        assertTrue(captor.getValue().getUuid().equals(userFrom.getUuid()));
        verify(userService).findUsersByUuids(anyListOf(String.class));
        verifyNoMoreInteractions(teamRepository, userService);
    }

    @Test
    public void deactivateTeamIfOneSlackUserInTextAndItIsNotFromUserExecutedCorrectly() {
        //given
        String text = String.format("%s", SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()));
        List<String> slackUsers = Arrays.asList(user1.getSlackUser(), userFrom.getSlackUser());
        List<User> users = Arrays.asList(userFrom, user1);
        given(userService.findUsersBySlackUsers(slackUsers)).willReturn(users);
        List<User> teamUsers = Arrays.asList(user1, user2, user3, user4);
        given(userService.findUsersByUuids(anyListOf(String.class))).willReturn(teamUsers);
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(
                user1.getSlackUser(), user2.getSlackUser(), user3.getSlackUser(), user4.getSlackUser()));
        Team team = new Team(teamUsers.stream().map(User::getUuid).collect(Collectors.toSet()), userFrom.getUuid(),
                "id", new Date(), new Date());
        given(teamRepository.deactivateTeam(any(DeactivateTeamRequest.class))).willReturn(team);

        //when
        Set<String> actual = new LinkedHashSet<>(teamService.deactivateTeam(userFrom.getSlackUser(), text));

        //then
        assertThat(actual, is(expected));
        verify(userService).findUsersBySlackUsers(slackUsers);
        ArgumentCaptor<DeactivateTeamRequest> captor = ArgumentCaptor.forClass(DeactivateTeamRequest.class);
        verify(teamRepository).deactivateTeam(captor.capture());
        assertTrue(captor.getValue().getFrom().equals(userFrom.getUuid()));
        assertTrue(captor.getValue().getUuid().equals(user1.getUuid()));
        verify(userService).findUsersByUuids(anyListOf(String.class));
        verifyNoMoreInteractions(teamRepository, userService);
    }

    @Test
    public void deactivateTeamIfSeveralSlackUsersInTextThrowsException() {
        String from = userFrom.getSlackUser();
        String text = String.format("%s %s",
                SlackUserHandler.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackUserHandler.wrapSlackUserInFullPattern(user2.getSlackUser()));
        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found %d slack user in your command. " +
                "But expect one slack user.", 2));

        teamService.deactivateTeam(from, text);
    }
}