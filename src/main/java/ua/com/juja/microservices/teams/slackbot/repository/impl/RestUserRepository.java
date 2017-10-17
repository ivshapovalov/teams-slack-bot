package ua.com.juja.microservices.teams.slackbot.repository.impl;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.users.UserUuidRequest;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;
import ua.com.juja.microservices.teams.slackbot.util.SlackNameHandler;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@Repository
@Slf4j
@Profile({"production", "default"})
public class RestUserRepository implements UserRepository {

    @Value("${users.endpoint.usersBySlackNames}")
    private String usersUrlFindUsersBySlackNames;
    @Value("${users.endpoint.usersByUuids}")
    private String usersUrlFindUsersByUuids;
    @Inject
    private GatewayClient gatewayClient;

    @Override
    public List<User> findUsersBySlackNames(List<String> slackNames) {
        SlackNameHandler.addAtToSlackNames(slackNames);
        UserSlackNameRequest userSlackNameRequest = new UserSlackNameRequest(slackNames);
        HttpEntity<UserSlackNameRequest> request = new HttpEntity<>(userSlackNameRequest, Utils.setupJsonHttpHeaders());
        List<User> users;
        try {
            log.debug("Send request '{}' to User service to url '{}'", request, usersUrlFindUsersBySlackNames);
            users = gatewayClient.getUsersBySlackNames(request);
            log.debug("Get response '{}' from User service", users);
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new UserExchangeException(error, ex);
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage().substring(ex.getMessage().indexOf("content:") + 8));
            throw new UserExchangeException(error, ex);
        }
        log.info("Found Users: '{}' by slackNames: '{}'", users, slackNames);
        return users;
    }

    @Override
    public List<User> findUsersByUuids(List<String> uuids) {
        UserUuidRequest userUuidRequest = new UserUuidRequest(uuids);
        HttpEntity<UserUuidRequest> request = new HttpEntity<>(userUuidRequest, Utils.setupJsonHttpHeaders());
        List<User> users;
        try {
            log.debug("Send request '{}' to User service to url '{}'", request, usersUrlFindUsersByUuids);
            users = gatewayClient.getUsersByUuids(request);
            log.debug("Get response '{}' from User service", users);
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new UserExchangeException(error, ex);
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage().substring(ex.getMessage().indexOf("content:") + 8));
            throw new UserExchangeException(error, ex);
        }
        log.info("Found Users:{} by uuids: '{}'", users, uuids);
        return users;
    }
}
