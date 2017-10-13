package ua.com.juja.microservices.teams.slackbot.repository.impl;

import net.javacrumbs.jsonunit.core.util.ResourceUtils;
import org.junit.Before;
import org.junit.BeforeClass;
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
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;
import ua.com.juja.microservices.utils.TestUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.javacrumbs.jsonunit.core.util.ResourceUtils.resource;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest({"eureka.client.registerWithEureka:false", "eureka.client.fetchRegistry:false"})
public class RestUserRepositoryTest {
    private static User user1;
    private static User user2;
    private static User user3;
    private static User user4;
    @Rule
    final public ExpectedException expectedException = ExpectedException.none();
    @Value("${users.endpoint.usersBySlackNames}")
    private String usersFindUsersBySlackNamesUrl;
    @Value("${users.endpoint.usersByUuids}")
    private String usersFindUsersByUuidsUrl;
    @Value("${gateway.name}")
    private String gatewayName;
    private String gatewayHost = "localhost";
    private int gatewayPort = 8765;
    @Inject
    private RestTemplate restTemplate;
    @MockBean
    private DiscoveryClient discoveryClient;
    @Inject
    private UserRepository userRepository;

    private MockRestServiceServer mockServer;

    @BeforeClass
    public static void oneTimeSetup() {
        user1 = new User("uuid1", "@slack1");
        user2 = new User("uuid2", "@slack2");
        user3 = new User("uuid3", "@slack3");
        user4 = new User("uuid4", "@slack4");
    }

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        mockDiscoveryServer();
    }

    private void mockDiscoveryServer() {
        ServiceInstance instanceInfo = mock(ServiceInstance.class);
        List<ServiceInstance> instances = new ArrayList<>(Collections.singleton(instanceInfo));

        when(discoveryClient.getInstances(any(String.class))).thenReturn(instances);
        when(instanceInfo.getHost()).thenReturn(gatewayHost);
        when(instanceInfo.getPort()).thenReturn(gatewayPort);
    }

    @Test
    public void findUsersBySlackNamesIfUserServerReturnsUsersCorrectly() throws IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + usersFindUsersBySlackNamesUrl;
        List<String> incorrectSlackNames = new ArrayList<>();
        incorrectSlackNames.add("slack1");
        incorrectSlackNames.add("@slack2");
        incorrectSlackNames.add("slack3");
        incorrectSlackNames.add("@slack4");

        List<User> expected = Arrays.asList(user1, user2, user3, user4);

        String jsonContentRequest = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryGetUsersBySlacknames.json"));

        String jsonContentExpectedResponse = TestUtils.convertToString(
                resource("response/responseUserRepositoryGetUsersBySlacknames.json"));
        mockServer.expect(requestTo(commandGatewayUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withSuccess(jsonContentExpectedResponse, MediaType.APPLICATION_JSON_UTF8));

        List<User> actual = userRepository.findUsersBySlackNames(incorrectSlackNames);

        assertThat(actual, is(expected));
    }

    @Test
    public void findUsersBySlackNamesIfUserServerReturnsException() throws IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + usersFindUsersBySlackNamesUrl;
        List<String> slackNames = new ArrayList<>();
        slackNames.add("slack1");
        slackNames.add("@slack2");
        slackNames.add("slack3");
        slackNames.add("@slack4");

        String jsonContentRequest = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryGetUsersBySlacknames.json"));

        String jsonContentExpectedResponse = TestUtils.convertToString(
                resource("response/responseUserRepositoryThrowsException.json"));
        mockServer.expect(requestTo(commandGatewayUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withBadRequest().body(jsonContentExpectedResponse));

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        userRepository.findUsersBySlackNames(slackNames);
    }

    @Test
    public void findUsersByUuidsIfUserServerReturnsUserCorrectly() throws IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + usersFindUsersByUuidsUrl;
        List<String> uuids = Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid());
        List<User> expected = Arrays.asList(user1, user2, user3, user4);
        String jsonContentRequest = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryGetUsersByUuids.json"));

        String jsonContentExpectedResponse = TestUtils.convertToString(
                resource("response/responseUserRepositoryGetUsersByUuids.json"));
        mockServer.expect(requestTo(commandGatewayUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withSuccess(jsonContentExpectedResponse, MediaType.APPLICATION_JSON_UTF8));

        List<User> actual = userRepository.findUsersByUuids(uuids);

        assertThat(actual, is(expected));
    }

    @Test
    public void findUsersByUuidsIfUserServerReturnsException() throws IOException {
        String commandGatewayUrl = "http://" + gatewayHost + ":" + gatewayPort + usersFindUsersByUuidsUrl;
        List<String> uuids = Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid());
        String jsonContentRequest = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryGetUsersByUuids.json"));
        String jsonContentExpectedResponse = TestUtils.convertToString(
                resource("response/responseUserRepositoryThrowsException.json"));
        mockServer.expect(requestTo(commandGatewayUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withBadRequest().body(jsonContentExpectedResponse));

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        userRepository.findUsersByUuids(uuids);
    }
}