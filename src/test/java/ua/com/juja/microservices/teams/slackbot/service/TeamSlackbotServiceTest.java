package ua.com.juja.microservices.teams.slackbot.service;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TeamSlackbotServiceTest {

    @Rule
    final public ExpectedException expectedException = ExpectedException.none();

    @MockBean
    private TeamService teamService;
    @MockBean
    private SlackNameHandlerService slackNameHandlerService;

    @Inject
    private TeamSlackbotService teamSlackbotService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void activateTeamExecutedCorrectly() {

        String text = "@slack1 @slack2 @slack3 @slack4";

        Set<String> members = new LinkedHashSet<>(Arrays.asList(new String[]{"uuid1", "uuid2", "uuid3", "uuid4"}));

        RichMessage expected = new RichMessage(String.format("Thanks, new Team for '%s' activated", text));
        Team activatedTeam = new Team(members);
        given(slackNameHandlerService.getUuidsFromText(text)).willReturn(members);
        given(teamService.activateTeam(any(TeamRequest.class))).willReturn(activatedTeam);

        RichMessage actual = teamSlackbotService.activateTeam(text);

        assertThat(actual.getText(), equalTo(expected.getText()));
        verify(slackNameHandlerService).getUuidsFromText(text);
        verify(teamService).activateTeam(any(TeamRequest.class));
        verifyNoMoreInteractions(slackNameHandlerService, teamService);
    }


}