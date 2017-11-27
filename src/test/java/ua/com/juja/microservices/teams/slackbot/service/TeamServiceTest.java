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
import ua.com.juja.microservices.teams.slackbot.util.SlackIdHandler;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
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
        user1 = new User("uuid1", "slack-id1");
        user2 = new User("uuid2", "slack-id2");
        user3 = new User("uuid3", "slack-id3");
        user4 = new User("uuid4", "slack-id4");
        userFrom = new User("uuid-from", "from-id");
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void activateTeamIfMembersSizeEqualsFourAndFromUserInTeamExecutedCorrectly() {
        String text = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackId(user1.getSlackId()),
                SlackIdHandler.wrapSlackId(user2.getSlackId()),
                SlackIdHandler.wrapSlackId(user3.getSlackId()),
                SlackIdHandler.wrapSlackId(userFrom.getSlackId()));
        Set<String> uuids = new LinkedHashSet<>(Arrays.asList(
                user1.getUuid(), user2.getUuid(), user3.getUuid(), userFrom.getUuid()));
        List<User> users = Arrays.asList(user1, user2, user3, userFrom);
        when(userService.findUsersBySlackIds(anyListOf(String.class))).thenReturn(users);
        Team expected = new Team(userFrom.getUuid(), uuids);
        given(teamRepository.activateTeam(any(ActivateTeamRequest.class))).willReturn(expected);

        Team actual = teamService.activateTeam(userFrom.getSlackId(), text);

        assertEquals(expected, actual);
        verify(userService).findUsersBySlackIds(anyListOf(String.class));
        ArgumentCaptor<ActivateTeamRequest> captor = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        verify(teamRepository).activateTeam(captor.capture());
        assertTrue(captor.getValue().getMembers().equals(uuids));
        assertTrue(captor.getValue().getFrom().equals(userFrom.getUuid()));
        verifyNoMoreInteractions(userService, teamRepository);
    }

    @Test
    public void activateTeamIfMembersSizeEqualsFourAndFromUserNotInTeamExecutedCorrectly() {
        String from = userFrom.getSlackId();
        String text = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackId(user1.getSlackId()),
                SlackIdHandler.wrapSlackId(user2.getSlackId()),
                SlackIdHandler.wrapSlackId(user3.getSlackId()),
                SlackIdHandler.wrapSlackId(user4.getSlackId()));
        Set<String> uuids = new LinkedHashSet<>(Arrays.asList(
                user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid()));
        List<User> users = Arrays.asList(user1, user2, user3, user4, userFrom);
        when(userService.findUsersBySlackIds(anyListOf(String.class))).thenReturn(users);
        Team expected = new Team(uuids);
        given(teamRepository.activateTeam(any(ActivateTeamRequest.class))).willReturn(expected);

        Team actual = teamService.activateTeam(from, text);

        assertEquals(expected, actual);
        verify(userService).findUsersBySlackIds(anyListOf(String.class));
        ArgumentCaptor<ActivateTeamRequest> captor = ArgumentCaptor.forClass(ActivateTeamRequest.class);
        verify(teamRepository).activateTeam(captor.capture());
        assertTrue(captor.getValue().getMembers().equals(uuids));
        assertTrue(captor.getValue().getFrom().equals(userFrom.getUuid()));
        verifyNoMoreInteractions(userService, teamRepository);
    }

    @Test
    public void activateTeamIfMembersSizeNotEqualsFourThrowsException() {
        String from = userFrom.getSlackId();
        String text = String.format("%s %s %s",
                SlackIdHandler.wrapSlackId(user1.getSlackId()),
                SlackIdHandler.wrapSlackId(user2.getSlackId()),
                SlackIdHandler.wrapSlackId(user3.getSlackId()));

        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found 3 slack id in your command." +
                " But size of the team must be %s.", TEAM_SIZE));

        teamService.activateTeam(from, text);
    }

    @Test
    public void activateTeamIfMembersOfRequestAndResponseOfTeamsServiceNotEqualsThrowsException() {
        String from = userFrom.getSlackId();
        String text = String.format("%s %s %s %s",
                SlackIdHandler.wrapSlackId(user1.getSlackId()),
                SlackIdHandler.wrapSlackId(user2.getSlackId()),
                SlackIdHandler.wrapSlackId(user3.getSlackId()),
                SlackIdHandler.wrapSlackId(user4.getSlackId()));
        List<String> slackIds = Arrays.asList(user1.getSlackId(), user2.getSlackId(), user3.getSlackId(), user4.getSlackId(), from);
        Set<String> requestMembers = new LinkedHashSet<>(Arrays.asList(
                user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid()));
        Set<String> responseMembers = new LinkedHashSet<>(Arrays.asList(
                user1.getUuid(), user2.getUuid(), user3.getUuid(), "uuid5"
        ));
        List<User> users = Arrays.asList(user1, user2, user3, user4, userFrom);
        when(userService.findUsersBySlackIds(slackIds)).thenReturn(users);
        Team activatedTeam = new Team(responseMembers);
        given(teamRepository.activateTeam(any(ActivateTeamRequest.class))).willReturn(activatedTeam);

        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage("Team members is not equals in request and response from Teams Service");

        try {
            teamService.activateTeam(from, text);
        } finally {
            verify(userService).findUsersBySlackIds(slackIds);
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

        teamService.activateTeam(userFrom.getSlackId(), null);
    }

    @Test
    public void activateTeamIfFromUserIsNullThrowsException() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("FromUserId must not be null!");

        teamService.activateTeam(null, "text");
    }

    @Test
    public void getTeamIfOneSlackIdInTextExecutedCorrectly() {
        String text = String.format("%s",
                SlackIdHandler.wrapSlackId(user1.getSlackId()));
        List<String> slackIdsInText = Collections.singletonList(user1.getSlackId());
        List<User> users = Collections.singletonList(user1);
        List<User> teamUsers = Arrays.asList(user1, user2, user3, user4);
        Set<String> uuids = teamUsers.stream().map(User::getUuid).collect(Collectors.toSet());
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(user1.getSlackId(), user2.getSlackId(), user3.getSlackId(),
                user4.getSlackId()));
        Team team = new Team(uuids);
        given(userService.findUsersBySlackIds(slackIdsInText)).willReturn(users);
        given(teamRepository.getTeam(user1.getUuid())).willReturn(team);
        given(userService.findUsersByUuids(anyListOf(String.class))).willReturn(teamUsers);

        Set<String> actual = teamService.getTeam(text);

        assertEquals(expected, actual);
        verify(userService).findUsersBySlackIds(slackIdsInText);
        verify(teamRepository).getTeam(user1.getUuid());
        verify(userService).findUsersByUuids(anyListOf(String.class));
        verifyNoMoreInteractions(teamRepository, userService);
    }

    @Test
    public void getTeamIfMoreThanOneSlackIdInTextThrowsException() {
        String text = String.format("%s %s",
                SlackIdHandler.wrapSlackId(user1.getSlackId()),
                SlackIdHandler.wrapSlackId(user2.getSlackId()));
        List<User> users = Arrays.asList(user1, user2);
        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found %d slack id in your command." +
                " But expect one slack id.", users.size()));

        teamService.getTeam(text);
    }

    @Test
    public void getTeamIfZeroSlackIdInTextThrowsException() {
        String text = "slack-id1 slack-id2";
        List<String> slackIdsInText = Collections.emptyList();
        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found %d slack id in your command." +
                " But expect one slack id.", slackIdsInText.size()));

        teamService.getTeam(text);
    }

    @Test
    public void deactivateTeamIfOneFromUserSlackIdInTextExecutedCorrectly() {
        String from = userFrom.getSlackId();
        String text = String.format("%s", SlackIdHandler.wrapSlackId(userFrom.getSlackId()));
        List<String> slackIds = Collections.singletonList(userFrom.getSlackId());
        List<User> users = Collections.singletonList(userFrom);
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(
                user1.getSlackId(), user2.getSlackId(), user3.getSlackId(), user4.getSlackId()));
        given(userService.findUsersBySlackIds(slackIds)).willReturn(users);
        List<User> teamUsers = Arrays.asList(user1, user2, user3, user4);
        given(userService.findUsersByUuids(anyListOf(String.class))).willReturn(teamUsers);
        Team team = new Team(teamUsers.stream().map(User::getUuid).collect(Collectors.toSet()));
        given(teamRepository.deactivateTeam(any(DeactivateTeamRequest.class))).willReturn(team);

        Set<String> actual = teamService.deactivateTeam(from, text);

        assertThat(actual, is(expected));
        verify(userService).findUsersBySlackIds(slackIds);
        ArgumentCaptor<DeactivateTeamRequest> captor = ArgumentCaptor.forClass(DeactivateTeamRequest.class);
        verify(teamRepository).deactivateTeam(captor.capture());
        assertTrue(captor.getValue().getFrom().equals(userFrom.getUuid()));
        assertTrue(captor.getValue().getUuid().equals(userFrom.getUuid()));
        verify(userService).findUsersByUuids(anyListOf(String.class));
        verifyNoMoreInteractions(teamRepository, userService);
    }

    @Test
    public void deactivateTeamIfOneSlackIdsInTextAndItIsNotFromUserExecutedCorrectly() {
        String text = String.format("%s", SlackIdHandler.wrapSlackId(user1.getSlackId()));
        List<String> slackIds = Arrays.asList(user1.getSlackId(), userFrom.getSlackId());
        List<User> users = Arrays.asList(userFrom, user1);
        given(userService.findUsersBySlackIds(slackIds)).willReturn(users);
        List<User> teamUsers = Arrays.asList(user1, user2, user3, user4);
        given(userService.findUsersByUuids(anyListOf(String.class))).willReturn(teamUsers);
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(
                user1.getSlackId(), user2.getSlackId(), user3.getSlackId(), user4.getSlackId()));
        Team team = new Team(teamUsers.stream().map(User::getUuid).collect(Collectors.toSet()));
        given(teamRepository.deactivateTeam(any(DeactivateTeamRequest.class))).willReturn(team);

        Set<String> actual = new LinkedHashSet<>(teamService.deactivateTeam(userFrom.getSlackId(), text));

        assertThat(actual, is(expected));
        verify(userService).findUsersBySlackIds(slackIds);
        ArgumentCaptor<DeactivateTeamRequest> captor = ArgumentCaptor.forClass(DeactivateTeamRequest.class);
        verify(teamRepository).deactivateTeam(captor.capture());
        assertTrue(captor.getValue().getFrom().equals(userFrom.getUuid()));
        assertTrue(captor.getValue().getUuid().equals(user1.getUuid()));
        verify(userService).findUsersByUuids(anyListOf(String.class));
        verifyNoMoreInteractions(teamRepository, userService);
    }

    @Test
    public void deactivateTeamIfSeveralSlackIdsInTextThrowsException() {
        String from = userFrom.getSlackId();
        String text = String.format("%s %s",
                SlackIdHandler.wrapSlackId(user1.getSlackId()),
                SlackIdHandler.wrapSlackId(user2.getSlackId()));
        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found %d slack id in your command. " +
                "But expect one slack id.", 2));

        teamService.deactivateTeam(from, text);
    }
}