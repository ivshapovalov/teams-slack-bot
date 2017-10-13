package ua.com.juja.microservices.teams.slackbot.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.users.UserUuidRequest;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;
import ua.com.juja.microservices.teams.slackbot.util.SlackNameHandler;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@Repository
@Slf4j
@Profile({"production", "default"})
public class RestUserRepository extends RestRepository implements UserRepository {
    private final RestTemplate restTemplate;
    @Value("${users.endpoint.usersBySlackNames}")
    private String usersUrlFindUsersBySlackNames;
    @Value("${users.endpoint.usersByUuids}")
    private String usersUrlFindUsersByUuids;

    @Inject
    public RestUserRepository(RestTemplate restTemplate, DiscoveryClient discoveryClient) {
        super(discoveryClient);
        this.restTemplate = restTemplate;
    }

    @Override
    public List<User> findUsersBySlackNames(List<String> slackNames) {
        String fullUsersUrlFindUsersBySlackNames = getCommandGatewayUrl(usersUrlFindUsersBySlackNames);
        SlackNameHandler.addAtToSlackNames(slackNames);
        UserSlackNameRequest userSlackNameRequest = new UserSlackNameRequest(slackNames);
        HttpEntity<UserSlackNameRequest> request = new HttpEntity<>(userSlackNameRequest, Utils.setupJsonHttpHeaders());
        List<User> users = getUsers(request, fullUsersUrlFindUsersBySlackNames);
        log.info("Found Users: '{}' by slackNames: '{}'", users, slackNames);
        return users;
    }

    @Override
    public List<User> findUsersByUuids(List<String> uuids) {
        String fullUsersUrlFindUsersByUuids = getCommandGatewayUrl(usersUrlFindUsersByUuids);
        UserUuidRequest userUuidRequest = new UserUuidRequest(uuids);
        HttpEntity<UserUuidRequest> request = new HttpEntity<>(userUuidRequest, Utils.setupJsonHttpHeaders());
        List<User> users = getUsers(request, fullUsersUrlFindUsersByUuids);
        log.info("Found Users:{} by uuids: '{}'", users, uuids);
        return users;
    }

    private <T> List<User> getUsers(HttpEntity<T> request, String userServiceURL) {
        List<User> users;
        try {
            log.debug("Send request '{}' to User service to url '{}'", request, userServiceURL);
            ResponseEntity<User[]> response = restTemplate.exchange(userServiceURL,
                    HttpMethod.POST, request, User[].class);
            log.debug("Get response '{}' from User service", response);
            users = Arrays.asList(response.getBody());
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new UserExchangeException(error, ex);
        }
        return users;
    }
}
