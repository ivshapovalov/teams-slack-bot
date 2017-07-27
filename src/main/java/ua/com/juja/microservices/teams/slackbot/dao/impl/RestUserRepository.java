package ua.com.juja.microservices.teams.slackbot.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.dao.UserRepository;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.DTO.SlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
@Qualifier("rest")
@Slf4j
public class RestUserRepository extends AbstractRestRepository implements UserRepository {

    private final RestTemplate restTemplate;

    @Value("${user.baseURL}")
    private String urlBase;
    @Value("${endpoint.userSearchBySlackName}")
    private String urlFindUsersBySlackNames;
    @Value("${endpoint.userSearchByUuids}")
    private String urlFindUsersByUuids;

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
        log.debug("Creating slackNameRequest and HttpEntity");
        SlackNameRequest slackNameRequest = new SlackNameRequest(slackNames);
        HttpEntity<SlackNameRequest> request = new HttpEntity<>(slackNameRequest, setupBaseHttpHeaders());

        List<UserDTO> result = new ArrayList<>();
        try {
            String userServiceURL = urlBase + urlFindUsersBySlackNames;
            log.debug("Started request to Users service url '{}'. Request is : '{}'", userServiceURL, request.toString
                    ());
            ResponseEntity<UserDTO[]> response = restTemplate.exchange(userServiceURL,
                    HttpMethod.POST, request, UserDTO[].class);
            log.debug("Finished request to Users service. Response is: '{}'", response.toString());
            result = Arrays.asList(response.getBody());
        } catch (HttpClientErrorException ex) {
            ApiError error = convertToApiError(ex);
            log.warn("Users service returned an error: '{}'", error);
            throw new UserExchangeException(error, ex);
        }
        log.info("Got UserDTO:{} for slackNames: {}", result, slackNames);
        return result;
    }

    @Override
    public List<UserDTO> findUsersByUuids(List<String> uuids) {
        log.debug("Received uids to convert : '{}'", uuids);

        log.debug("Creating slackNameRequest and HttpEntity");
        SlackNameRequest slackNameRequest = new SlackNameRequest(uuids);
        HttpEntity<SlackNameRequest> request = new HttpEntity<>(slackNameRequest, setupBaseHttpHeaders());

        List<UserDTO> result = new ArrayList<>();
        try {
            String userServiceURL = urlBase + urlFindUsersByUuids;
            log.debug("Started request to Users service url '{}'. Request is : '{}'", userServiceURL, request.toString
                    ());
            ResponseEntity<UserDTO[]> response = restTemplate.exchange(userServiceURL,
                    HttpMethod.POST, request, UserDTO[].class);
            log.debug("Finished request to Users service. Response is: '{}'", response.toString());
            result = Arrays.asList(response.getBody());
        } catch (HttpClientErrorException ex) {
            ApiError error = convertToApiError(ex);
            log.warn("Users service returned an error: '{}'", error);
            throw new UserExchangeException(error, ex);
        }
        log.info("Got UserDTO:{} for uuids: {}", result, uuids);
        return result;
    }
}
