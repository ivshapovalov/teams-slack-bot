package ua.com.juja.microservices.teams.slackbot.repository.impl;

import net.javacrumbs.jsonunit.core.util.ResourceUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.teams.ActivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.DeactivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.Team;
import ua.com.juja.microservices.teams.slackbot.repository.TeamRepository;
import ua.com.juja.microservices.utils.TestUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest({"eureka.client.registerWithEureka:false", "eureka.client.fetchRegistry:false"})
public class RestTeamRepositoryTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @MockBean
    private DiscoveryClient discoveryClient;
    @Inject
    private RestTemplate restTemplate;
    @Inject
    private TeamRepository teamRepository;
    private MockRestServiceServer mockServer;
    @Value("${teams.endpoint.activateTeam}")
    private String teamsActivateTeamUrl;
    @Value("${teams.endpoint.deactivateTeam}")
    private String teamsDeactivateTeamUrl;
    @Value("${teams.endpoint.getTeam}")
    private String teamsGetTeamUrl;

    @Value("${gateway.name}")
    private String gatewayName;
    private String gatewayHost = "localhost";
    private int gatewayPort = 8765;

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        mockDiscoveryServerSuccessful();
    }

    private void mockDiscoveryServerSuccessful() {
        ServiceInstance instanceInfo = mock(ServiceInstance.class);
        List<ServiceInstance> instances = new ArrayList<>(Collections.singleton(instanceInfo));

        when(discoveryClient.getInstances(any(String.class))).thenReturn(instances);
        when(instanceInfo.getHost()).thenReturn(gatewayHost);
        when(instanceInfo.getPort()).thenReturn(gatewayPort);
    }

    @Test
    public void activateTeamWhenEurekaCacheIsNull() throws IOException {
        String uuidFrom = "uuid-from";
        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        ActivateTeamRequest activateTeamRequest = new ActivateTeamRequest(uuidFrom, members);
        when(discoveryClient.getInstances(any(String.class))).thenReturn(null);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(containsString("Eureka may be not contain gateway instance"));

        teamRepository.activateTeam(activateTeamRequest);
    }

    @Test
    public void activateTeamSendRequestToRemoteTeamsServerAndReturnActivatedTeamExecutedCorrectly() throws IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + teamsActivateTeamUrl;
        String uuidFrom = "uuid-from";
        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        ActivateTeamRequest activateTeamRequest = new ActivateTeamRequest(uuidFrom, members);

        String expectedJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeamAndFromUserNotInText.json"));
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryActivateTeamIfUsersNotInActiveTeam.json"));
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(commandGatewayUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedJsonRequestBody)))
                .andRespond(withSuccess(expectedJsonResponseBody, MediaType.APPLICATION_JSON));

        Team actual = teamRepository.activateTeam(activateTeamRequest);

        assertNotNull(actual);
        assertThat(actual.getMembers(), is(members));
    }

    @Test
    public void activateTeamSendRequestToRemoteTeamsServerWhichReturnsErrorThrowsException() throws IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + teamsActivateTeamUrl;
        String uuidFrom = "uuid-from";
        Set<String> members = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        ActivateTeamRequest activateTeamRequest = new ActivateTeamRequest(uuidFrom, members);
        String expectedJsonRequestBody = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestTeamRepositoryActivateTeamIfUsersNotInActiveTeamAndFromUserNotInText.json"));
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryActivateTeamIfUsersInActiveTeamThrowsException.json"));
        String expectedRequestHeader = "application/json";

        mockServer.expect(requestTo(commandGatewayUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedJsonRequestBody)))
                .andRespond(withBadRequest().body(expectedJsonResponseBody));
        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, but the user already exists in team"));

        teamRepository.activateTeam(activateTeamRequest);
    }

    @Test
    public void getTeamSendRequestToRemoteTeamsServerAndReturnTeamExecutedCorrectly() throws
            IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + teamsGetTeamUrl;
        String uuid = "uuid";
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetAndDeactivateTeamIfUsersInActiveTeam.json"));
        mockServer.expect(requestTo(commandGatewayUrl + "/" + uuid))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(expectedJsonResponseBody, MediaType.APPLICATION_JSON));

        Team actual = teamRepository.getTeam(uuid);

        assertNotNull(actual);
        assertThat(actual.getMembers(), is(expected));
    }

    @Test
    public void getTeamSendRequestToRemoteTeamsServerWhichReturnsErrorThrowsException() throws IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + teamsGetTeamUrl;
        String uuid = "uuid";
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetAndDeactivateTeamIfUsersNotInActiveTeamThrowsException.json"));
        mockServer.expect(requestTo(commandGatewayUrl + "/" + uuid))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest().body(expectedJsonResponseBody));
        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("You cannot get/deactivate team if the user not a member of any team!"));

        teamRepository.getTeam(uuid);
    }

    @Test
    public void getTeamRemoteTeamsServerReturnsErrorWhichUnableToConvertToApiErrorThrowsTeamException() throws
            IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + teamsGetTeamUrl;
        String uuid = "uuid";
        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetTeamUnknownException.json"));
        mockServer.expect(requestTo(commandGatewayUrl + "/" + uuid))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest().body(expectedJsonResponseBody));

        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        teamRepository.getTeam(uuid);
    }

    @Test
    public void deactivateTeamSendRequestToRemoteTeamsServerAndReturnDeactivatedTeamExecutedCorrectly() throws
            IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + teamsDeactivateTeamUrl;
        String uuidFrom = "uuid-from";
        String uuid = "uuid2";
        DeactivateTeamRequest deactivateTeamRequest = new DeactivateTeamRequest(uuidFrom, uuid);
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("uuid1", "uuid2", "uuid3", "uuid4"));

        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetAndDeactivateTeamIfUsersInActiveTeam.json"));
        mockServer.expect(requestTo(commandGatewayUrl))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(expectedJsonResponseBody, MediaType.APPLICATION_JSON));

        Team actual = teamRepository.deactivateTeam(deactivateTeamRequest);

        assertNotNull(actual);
        assertThat(actual.getMembers(), is(expected));
    }

    @Test
    public void deactivateTeamSendRequestToRemoteTeamsServerWhichReturnsErrorThrowsException() throws IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + teamsDeactivateTeamUrl;
        String uuidFrom = "uuid-from";
        String uuid = "uuid2";
        DeactivateTeamRequest deactivateTeamRequest = new DeactivateTeamRequest(uuidFrom, uuid);

        String expectedJsonResponseBody = TestUtils.convertToString(ResourceUtils.resource
                ("response/responseTeamRepositoryGetAndDeactivateTeamIfUsersNotInActiveTeamThrowsException.json"));
        mockServer.expect(requestTo(commandGatewayUrl))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withBadRequest().body(expectedJsonResponseBody));

        expectedException.expect(TeamExchangeException.class);
        expectedException.expectMessage(containsString("You cannot get/deactivate team if the user not a member of any team!"));

        teamRepository.deactivateTeam(deactivateTeamRequest);
    }
}