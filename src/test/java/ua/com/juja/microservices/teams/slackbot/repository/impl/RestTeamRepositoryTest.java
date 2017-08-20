package ua.com.juja.microservices.teams.slackbot.repository.impl;

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
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.repository.TeamRepository;
import ua.com.juja.microservices.utils.TestUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
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
    public final ExpectedException expectedException = ExpectedException.none();
    @Inject
    private TeamRepository teamRepository;
    @Inject
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    @Value("${teams.rest.api.version}")
    private String teamsRestApiVersion;
    @Value("${teams.baseURL}")
    private String teamsBaseUrl;
    @Value("${teams.endpoint.activateTeam}")
    private String teamsActivateTeamUrl;
    @Value("${teams.endpoint.getTeam}")
    private String teamsGetTeamUrl;

    private String teamsFullActivateTeamUrl;
    private String teamsFullGetTeamUrl;

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        teamsFullActivateTeamUrl = teamsBaseUrl + "/" + teamsRestApiVersion + teamsActivateTeamUrl;
        teamsFullGetTeamUrl = teamsBaseUrl + "/" + teamsRestApiVersion + teamsGetTeamUrl;
    }

    @Test
    public void activateTeamSendRequestToRemoteTeamsServerAndReturnActivatedTeamExecutedCorrectly() throws IOException {

        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        TeamRequest teamRequest = new TeamRequest(members);

        String expectedJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(teamsFullActivateTeamUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedJsonRequestBody)))
                .andRespond(withSuccess(expectedJsonResponseBody, MediaType.APPLICATION_JSON));

        Team actual = teamRepository.activateTeam(teamRequest);

        mockServer.verify();
        assertNotNull(actual);
        assertThat(actual.getMembers(), is(members));
    }

    @Test
    public void activateTeamSendRequestToRemoteTeamsServerWhichReturnsErrorThrowsException() throws IOException {

        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        TeamRequest teamRequest = new TeamRequest(members);
        String expectedJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryActivateTeamIfUsersInActiveTeamThrowsException.json"));
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(teamsFullActivateTeamUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedJsonRequestBody)))
                .andRespond(withBadRequest().body(expectedJsonResponseBody));
        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, but the user already exists in team"));

        teamRepository.activateTeam(teamRequest);

        mockServer.verify();
    }

    @Test
    public void getTeamSendRequestToRemoteTeamsServerAndReturnTeamExecutedCorrectly() throws
            IOException {
        String uuid = "uuid";
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetTeamIfUsersInActiveTeam.json"));
        mockServer.expect(requestTo(teamsFullGetTeamUrl + "/" + uuid))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(expectedJsonResponseBody, MediaType.APPLICATION_JSON));

        Team actual = teamRepository.getTeam(uuid);

        mockServer.verify();
        assertNotNull(actual);
        assertThat(actual.getMembers(), is(expected));
    }

    @Test
    public void getTeamSendRequestToRemoteTeamsServerWhichReturnsErrorThrowsException() throws IOException {
        String uuid = "uuid";
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetTeamIfUsersNotInActiveTeamThrowsException.json"));
        mockServer.expect(requestTo(teamsFullGetTeamUrl + "/" + uuid))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest().body(expectedJsonResponseBody));
        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("You cannot get/deactivate team if user not in team"));

        teamRepository.getTeam(uuid);

        mockServer.verify();
    }

    @Test
    public void getTeamRemoteTeamsServerReturnsErrorWhichUnableToConvertToApiErrorThrowsTeamException() throws
            IOException {
        String uuid = "uuid";
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetTeamUnknownException.json"));
        mockServer.expect(requestTo(teamsFullGetTeamUrl + "/" + uuid))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest().body(expectedJsonResponseBody));

        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        teamRepository.getTeam(uuid);

        mockServer.verify();
    }

}