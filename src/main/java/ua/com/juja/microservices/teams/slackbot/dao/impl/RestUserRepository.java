package ua.com.juja.microservices.teams.slackbot.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.dao.UserRepository;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.SlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.UserDTO;

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
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        log.debug("Received slackNames to convert : '{}'", slackNames);
        for (int i = 0; i < slackNames.size(); i++) {
            if (!slackNames.get(i).startsWith("@")) {
                log.debug("Add '@' to slackName : '{}'", slackNames.get(i));
                String slackName = slackNames.get(i);
                slackNames.set(i, "@" + slackName);
            }
        }
        log.debug("Started creating slackNameRequest and HttpEntity");
        SlackNameRequest slackNameRequest = new SlackNameRequest(slackNames);
        HttpEntity<SlackNameRequest> request = new HttpEntity<>(slackNameRequest, setupBaseHttpHeaders());
        log.debug("Finished creating slackNameRequest and HttpEntity");

        List<UserDTO> users;
        try {
            String userServiceURL = userUrlBase + userUrlFindUsersBySlackNames;
            log.debug("Started request to Users service url '{}'. Request is : '{}'", userServiceURL, request.toString
                    ());
            ResponseEntity<UserDTO[]> response = restTemplate.exchange(userServiceURL,
                    HttpMethod.POST, request, UserDTO[].class);
            log.debug("Finished request to Users service. Response is: '{}'", response.toString());
            users = Arrays.asList(response.getBody());
        } catch (HttpClientErrorException ex) {
            //TODO покрыть тестами
            ApiError error = convertToApiError(ex);
            log.warn("Users service returned an error: '{}'", error);
            throw new UserExchangeException(error, ex);
        }
        log.info("Found UserDTO: '{}' for slackNames: {}", users, slackNames);
        return users;
    }

    @Override
    public List<UserDTO> findUsersByUuids(List<String> uuids) {
        log.debug("Received uids to convert : '{}'", uuids);
        log.debug("Started creating slackNameRequest and HttpEntity");
        SlackNameRequest slackNameRequest = new SlackNameRequest(uuids);
        HttpEntity<SlackNameRequest> request = new HttpEntity<>(slackNameRequest, setupBaseHttpHeaders());
        log.debug("Finished creating slackNameRequest and HttpEntity");

        List<UserDTO> result;
        try {
            String userServiceURL = userUrlBase + userUrlFindUsersByUuids;
            log.debug("Started request to Users service url '{}'. Request is : '{}'", userServiceURL, request.toString
                    ());
            ResponseEntity<UserDTO[]> response = restTemplate.exchange(userServiceURL,
                    HttpMethod.POST, request, UserDTO[].class);
            log.debug("Finished request to Users service. Response is: '{}'", response.toString());
            result = Arrays.asList(response.getBody());
        } catch (HttpClientErrorException ex) {
            //TODO покрыть тестами
            ApiError error = convertToApiError(ex);
            log.warn("Users service returned an error: '{}'", error);
            throw new UserExchangeException(error, ex);
        }
        log.info("Found UserDTO:{} for uuids: {}", result, uuids);
        return result;
    }
}
