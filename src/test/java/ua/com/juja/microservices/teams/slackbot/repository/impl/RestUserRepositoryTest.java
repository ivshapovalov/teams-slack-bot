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
import java.util.List;

import static net.javacrumbs.jsonunit.core.util.ResourceUtils.resource;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
@SpringBootTest
public class RestUserRepositoryTest {
    private static User user1;
    private static User user2;
    private static User user3;
    private static User user4;
    @Rule
    final public ExpectedException expectedException = ExpectedException.none();
    @Inject
    private UserRepository userRepository;
    @Inject
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    @Value("${users.endpoint.usersBySlackIds}")
    private String usersFindUsersBySlackIdsUrl;
    @Value("${users.endpoint.usersByUuids}")
    private String usersFindUsersByUuidsUrl;

    @BeforeClass
    public static void oneTimeSetup() {
        user1 = new User("uuid1", "slack-id1");
        user2 = new User("uuid2", "slack-id2");
        user3 = new User("uuid3", "slack-id3");
        user4 = new User("uuid4", "slack-id4");
    }

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void findUsersBySlackIdsIfUserServerReturnsUsersCorrectly() throws IOException {

        List<String> slackIds = new ArrayList<>();
        slackIds.add("slack-id1");
        slackIds.add("slack-id2");
        slackIds.add("slack-id3");
        slackIds.add("slack-id4");

        List<User> expected = Arrays.asList(user1, user2, user3, user4);

        String jsonContentRequest = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryFindUsersBySlackIds.json"));

        String jsonContentExpectedResponse = TestUtils.convertToString(
                resource("response/responseUserRepositoryFindUsersBySlackIds.json"));
        mockServer.expect(requestTo(usersFindUsersBySlackIdsUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withSuccess(jsonContentExpectedResponse, MediaType.APPLICATION_JSON_UTF8));

        List<User> actual = userRepository.findUsersBySlackIds(slackIds);

        assertThat(actual, is(expected));
    }

    @Test
    public void findUsersBySlackIdsIfUserServerReturnsException() throws IOException {

        List<String> slackIds = new ArrayList<>();
        slackIds.add("slack-id1");
        slackIds.add("slack-id2");
        slackIds.add("slack-id3");
        slackIds.add("slack-id4");

        String jsonContentRequest = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryFindUsersBySlackIds.json"));

        String jsonContentExpectedResponse = TestUtils.convertToString(
                resource("response/responseUserRepositoryThrowsException.json"));
        mockServer.expect(requestTo(usersFindUsersBySlackIdsUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withBadRequest().body(jsonContentExpectedResponse));

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        userRepository.findUsersBySlackIds(slackIds);
    }

    @Test
    public void findUsersByUuidsIfUserServerReturnsUserCorrectly() throws IOException {
        List<String> uuids = Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid());
        List<User> expected = Arrays.asList(user1, user2, user3, user4);
        String jsonContentRequest = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryFindUsersByUuids.json"));

        String jsonContentExpectedResponse = TestUtils.convertToString(
                resource("response/responseUserRepositoryFindUsersByUuids.json"));
        mockServer.expect(requestTo(usersFindUsersByUuidsUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withSuccess(jsonContentExpectedResponse, MediaType.APPLICATION_JSON_UTF8));

        List<User> actual = userRepository.findUsersByUuids(uuids);

        assertThat(actual, is(expected));
    }

    @Test
    public void findUsersByUuidsIfUserServerReturnsException() throws IOException {
        List<String> uuids = Arrays.asList(user1.getUuid(), user2.getUuid(),
                user3.getUuid(), user4.getUuid());
        String jsonContentRequest = TestUtils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryFindUsersByUuids.json"));
        String jsonContentExpectedResponse = TestUtils.convertToString(
                resource("response/responseUserRepositoryThrowsException.json"));
        mockServer.expect(requestTo(usersFindUsersByUuidsUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withBadRequest().body(jsonContentExpectedResponse));

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        userRepository.findUsersByUuids(uuids);
    }
}