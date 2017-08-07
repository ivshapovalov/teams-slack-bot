package ua.com.juja.microservices.teams.slackbot.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.repository.TeamRepository;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
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

    @Rule
    final public ExpectedException expectedException = ExpectedException.none();
    private final User user1 = new User("uuid1", "@slack1");
    private final User user2 = new User("uuid2", "@slack2");
    private final User user3 = new User("uuid3", "@slack3");
    private final User user4 = new User("uuid4", "@slack4");
    @MockBean
    private TeamRepository teamRepository;
    @MockBean
    private UserService userService;
    @Inject
    private TeamService teamService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void activateTeamExecutedCorrectly() {

        String text = "@slack1 @slack2 @slack3 @slack4";

        Set<String> uuids = new LinkedHashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid(),
                user4.getUuid()));
        List<User> users = Arrays.asList(user1, user2, user3, user4);

        Team expected = new Team(uuids);
        when(userService.findUsersBySlackNames(anyListOf(String.class))).thenReturn(users);
        given(teamRepository.activateTeam(any(TeamRequest.class))).willReturn(expected);

        Team actual = teamService.activateTeam(text);

        assertEquals(expected, actual);
        verify(userService).findUsersBySlackNames(anyListOf(String.class));
        verify(teamRepository).activateTeam(any(TeamRequest.class));
        verifyNoMoreInteractions(userService, teamRepository);
    }

    @Test
    public void activateTeamIfMembersSizeNotEqualsFourThrowsException() {

        String text = "@slack1 @slack2 @slack3";

        Set<String> members = new LinkedHashSet<>(Arrays.asList(user1.getUuid(), user2.getUuid(), user3.getUuid()));
        List<User> users = Arrays.asList(user1, user2, user3);

        when(userService.findUsersBySlackNames(anyListOf(String.class))).thenReturn(users);
        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found %d slack names in your command." +
                " But size of the team must be %s.", members.size(), TEAM_SIZE));

        teamService.activateTeam(text);

        verify(userService).findUsersBySlackNames(anyListOf(String.class));
        verifyNoMoreInteractions(userService, teamRepository);
    }

    @Test
    public void activateTeamIfTextIsNullThrowsException() {

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Text must not be null!");

        teamService.activateTeam(null);
        verifyNoMoreInteractions(userService, teamRepository);
    }

}