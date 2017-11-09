package ua.com.juja.microservices.teams.slackbot.repository.impl;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.users.UserUuidRequest;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;
import ua.com.juja.microservices.teams.slackbot.repository.feign.UsersClient;
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
    @Inject
    private UsersClient usersClient;

    @Override
    public List<User> findUsersBySlackNames(List<String> slackNames) {
        UserSlackNameRequest userSlackNameRequest = new UserSlackNameRequest(slackNames);

        List<User> users;
        try {
            users = usersClient.findUsersBySlackNames(userSlackNameRequest);
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage());
            throw new UserExchangeException(error, ex);
        }
        log.info("Found Users: '{}' by slackNames: '{}'", users, slackNames);
        return users;
    }

    @Override
    public List<User> findUsersByUuids(List<String> uuids) {
        UserUuidRequest userUuidRequest = new UserUuidRequest(uuids);
        List<User> users;
        try {
            users = usersClient.findUsersByUuids(userUuidRequest);
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage());
            throw new UserExchangeException(error, ex);
        }
        log.info("Found Users:{} by uuids: '{}'", users, uuids);
        return users;
    }
}
