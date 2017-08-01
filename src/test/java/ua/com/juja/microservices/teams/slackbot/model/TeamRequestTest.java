package ua.com.juja.microservices.teams.slackbot.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Ivan Shapovalov
 */
public class TeamRequestTest {
    private static final int TEAM_SIZE = 4;
    @Rule
    final public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void createTeamRequestIfMembersSizeEqualsFourExecutedCorrectly() {
        Set<String> members = new HashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));

        TeamRequest teamRequest = new TeamRequest(members);

        assertNotNull(teamRequest);
        assertThat(teamRequest.getMembers(), is(members));
    }

    @Test
    public void createTeamRequestIfMembersSizeIsZeroThrowsException() {
        Set<String> members = new HashSet<>();

        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We didn't find slack name in your command." +
                " You must write %s user's slack names for activate team.", TEAM_SIZE));

        new TeamRequest(members);
    }

    @Test
    public void createTeamRequestIfMembersSizeNotEqualsFourThrowsException() {
        Set<String> members = new HashSet<>(Arrays.asList("uuid1", "uuid2"));

        expectedException.expect(WrongCommandFormatException.class);
        expectedException.expectMessage(String.format("We found %d slack names in your command." +
                " But size of the team must be %s.", members.size(), TEAM_SIZE));

        new TeamRequest(members);
    }
}