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
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    @Rule
    final public ExpectedException expectedException = ExpectedException.none();
    @Inject
    private UserRepository userRepository;
    @Inject
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    @Value("${user.baseURL}")
    private String userBaseUrl;
    @Value("${endpoint.userSearchBySlackName}")
    private String userFindUsersBySlackNamesUrl;
    @Value("${endpoint.userSearchByUuids}")
    private String userFindUsersByUuidsUrl;

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void findUsersBySlackNamesUserServerReturnsUserDTOCorrectly() throws IOException {

        List<String> incorrectSlackNames = new ArrayList<>();
        incorrectSlackNames.add("user1");
        incorrectSlackNames.add("@user2");
        incorrectSlackNames.add("user3");
        incorrectSlackNames.add("@user4");

        List<String> correctSlackNames = new ArrayList<>();
        correctSlackNames.add("@user1");
        correctSlackNames.add("@user2");
        correctSlackNames.add("@user3");
        correctSlackNames.add("@user4");

        final int[] number = {1};
        List<User> expected = correctSlackNames.stream().map(slackName -> new User(String.valueOf(number[0]++), slackName))
                .collect(Collectors.toList());

        String jsonContentRequest = Utils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryGetUsersBySlacknames.json"));

        String jsonContentExpectedResponse = Utils.convertToString(
                resource("response/responseUserRepositoryGetUsersBySlacknames.json"));
        mockServer.expect(requestTo(userBaseUrl + userFindUsersBySlackNamesUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withSuccess(jsonContentExpectedResponse, MediaType.APPLICATION_JSON_UTF8));

        List<User> actual = userRepository.findUsersBySlackNames(incorrectSlackNames);

        mockServer.verify();
        assertThat(actual, is(expected));
    }


    @Test
    public void findUsersBySlackNamesUserServerReturnException() throws IOException {

        List<String> slackNames = new ArrayList<>();
        slackNames.add("user1");
        slackNames.add("@user2");
        slackNames.add("user3");
        slackNames.add("@user4");

        String jsonContentRequest = Utils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryGetUsersBySlacknames.json"));

        String jsonContentExpectedResponse = Utils.convertToString(
                resource("response/responseUserRepositoryThrowsException.json"));
        mockServer.expect(requestTo(userBaseUrl + userFindUsersBySlackNamesUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withBadRequest().body(jsonContentExpectedResponse));

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        userRepository.findUsersBySlackNames(slackNames);

    }

    @Test
    public void findUsersByUuidsReturnUserDTOCorrectly() throws IOException {

        List<String> uuids = new ArrayList<>();
        uuids.add("1");
        uuids.add("2");
        uuids.add("3");
        uuids.add("4");
        List<User> expected = uuids.stream().map(uuid -> new User(uuid, "@user" + uuid))
                .collect(Collectors.toList());

        String jsonContentRequest = Utils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryGetUsersByUuids.json"));

        String jsonContentExpectedResponse = Utils.convertToString(
                resource("response/responseUserRepositoryGetUsersByUuids.json"));
        mockServer.expect(requestTo(userBaseUrl + userFindUsersByUuidsUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withSuccess(jsonContentExpectedResponse, MediaType.APPLICATION_JSON_UTF8));

        List<User> actual = userRepository.findUsersByUuids(uuids);

        mockServer.verify();
        assertThat(actual, is(expected));
    }

    @Test
    public void findUsersByUuidsUserServerReturnException() throws IOException {

        List<String> uuids = new ArrayList<>();
        uuids.add("1");
        uuids.add("2");
        uuids.add("3");
        uuids.add("4");

        String jsonContentRequest = Utils.convertToString(ResourceUtils.resource
                ("request/requestUserRepositoryGetUsersByUuids.json"));

        String jsonContentExpectedResponse = Utils.convertToString(
                resource("response/responseUserRepositoryThrowsException.json"));
        mockServer.expect(requestTo(userBaseUrl + userFindUsersByUuidsUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(jsonContentRequest))
                .andRespond(withBadRequest().body(jsonContentExpectedResponse));

        expectedException.expect(UserExchangeException.class);
        expectedException.expectMessage(containsString("Sorry, User server return an error"));

        userRepository.findUsersByUuids(uuids);
    }
}