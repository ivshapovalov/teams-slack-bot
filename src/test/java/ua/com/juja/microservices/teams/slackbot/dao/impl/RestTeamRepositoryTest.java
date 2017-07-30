package ua.com.juja.microservices.teams.slackbot.dao.impl;

import net.javacrumbs.jsonunit.core.util.ResourceUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.dao.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RestTeamRepositoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Inject
    private TeamRepository teamRepository;
    @Inject
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    @Value("${rest.api.version}")
    private String restApiVersion = "v1";
    @Value("${teams.baseURL}")
    private String teamsBaseUrl;
    @Value("${endpoint.activateTeam}")
    private String teamsActivateTeamUrl;

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void activateTeamSendRequestToRemoteTeamsServerAndReturnActivatedTeamExecutedCorrectly() throws IOException {
        String teamsServiceURL = teamsBaseUrl + restApiVersion + teamsActivateTeamUrl;

        Set<String> members = new LinkedHashSet<>(Arrays.asList(new String[]{"uuid1", "uuid2", "uuid3", "uuid4"}));
        TeamRequest teamRequest = new TeamRequest(members);

        String expectedJsonRequestBody = Utils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        String expectedJsonResponseBody = Utils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(teamsServiceURL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedJsonRequestBody)))
                .andRespond(withSuccess(expectedJsonResponseBody, MediaType.APPLICATION_JSON));
        //when
        Team actual = teamRepository.activateTeam(teamRequest);

        // then
        mockServer.verify();
        assertThat(actual.getMembers(), is(teamRequest.getMembers()));
    }

    @Test
    public void activateTeamSendRequestToRemoteTeamsServerWhichReturnsErrorThrowsException() throws IOException {
        String teamsServiceURL = teamsBaseUrl + restApiVersion + teamsActivateTeamUrl;

        Set<String> members = new LinkedHashSet<>(Arrays.asList(new String[]{"uuid1", "uuid2", "uuid3",
                "uuid4"}));
        TeamRequest teamRequest = new TeamRequest(members);

        String expectedJsonRequestBody = Utils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        String expectedJsonResponseBody = Utils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryActivateTeamIfUsersInActiveTeamThrowsException.json"));
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(teamsServiceURL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedJsonRequestBody)))
                .andRespond(withBadRequest().body(expectedJsonResponseBody));

        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, but the user already exists in team"));

        teamRepository.activateTeam(teamRequest);

    }
}