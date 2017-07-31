package ua.com.juja.microservices.teams.slackbot.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.model.UserSlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.UserUuidRequest;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@Repository
@Slf4j
@Profile({"production", "default"})
public class RestUserRepository extends AbstractRestRepository implements UserRepository {
    private final RestTemplate restTemplate;

    @Value("${user.baseURL}")
    private String userUrlBase;
    @Value("${endpoint.userSearchBySlackName}")
    private String userUrlFindUsersBySlackNames;
    @Value("${endpoint.userSearchByUuids}")
    private String userUrlFindUsersByUuids;

    @Inject
    public RestUserRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<User> findUsersBySlackNames(List<String> slackNames) {
        log.debug("Received slackNames to convert : '{}'", slackNames);
        for (int i = 0; i < slackNames.size(); i++) {
            if (!slackNames.get(i).startsWith("@")) {
                log.debug("Add '@' to slackName : '{}'", slackNames.get(i));
                String slackName = slackNames.get(i);
                slackNames.set(i, "@" + slackName);
            }
        }
        log.debug("Started creating userSlackNameRequest and HttpEntity");
        UserSlackNameRequest userSlackNameRequest = new UserSlackNameRequest(slackNames);
        HttpEntity<UserSlackNameRequest> request = new HttpEntity<>(userSlackNameRequest, setupBaseHttpHeaders());
        log.debug("Finished creating userSlackNameRequest and HttpEntity");

        List<User> users;
        try {
            String userServiceURL = userUrlBase + userUrlFindUsersBySlackNames;
            log.debug("Started request to Users service url '{}'. Request is : '{}'", userServiceURL, request.toString
                    ());
            ResponseEntity<User[]> response = restTemplate.exchange(userServiceURL,
                    HttpMethod.POST, request, User[].class);
            log.debug("Finished request to Users service. Response is: '{}'", response.toString());
            users = Arrays.asList(response.getBody());
        } catch (HttpClientErrorException ex) {
            ApiError error = convertToApiError(ex);
            log.warn("Users service returned an error: '{}'", error);
            throw new UserExchangeException(error, ex);
        }
        log.info("Found User: '{}' for slackNames: {}", users, slackNames);
        return users;
    }

    @Override
    public List<User> findUsersByUuids(List<String> uuids) {
        log.debug("Received uids to convert : '{}'", uuids);
        log.debug("Started creating userUuidsRequest and HttpEntity");
        UserUuidRequest userUuidRequest = new UserUuidRequest(uuids);
        HttpEntity<UserUuidRequest> request = new HttpEntity<>(userUuidRequest, setupBaseHttpHeaders());
        log.debug("Finished creating userUuidsRequest and HttpEntity");

        List<User> result;
        try {
            String userServiceURL = userUrlBase + userUrlFindUsersByUuids;
            log.debug("Started request to Users service url '{}'. Request is : '{}'", userServiceURL, request.toString
                    ());
            ResponseEntity<User[]> response = restTemplate.exchange(userServiceURL,
                    HttpMethod.POST, request, User[].class);
            log.debug("Finished request to Users service. Response is: '{}'", response.toString());
            result = Arrays.asList(response.getBody());
        } catch (HttpClientErrorException ex) {
            ApiError error = convertToApiError(ex);
            log.warn("Users service returned an error: '{}'", error);
            throw new UserExchangeException(error, ex);
        }
        log.info("Found User:{} for uuids: {}", result, uuids);
        return result;
    }
}
