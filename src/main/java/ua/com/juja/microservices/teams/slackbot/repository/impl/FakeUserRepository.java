package ua.com.juja.microservices.teams.slackbot.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivan Shapovalov
 */
@Repository
@Slf4j
@Profile("test")
public class FakeUserRepository implements UserRepository {

    private static final Map<String, String> ALL_USERS = new HashMap<>();

    static {
        for (int i = 97; i < 122; i++) {
            ALL_USERS.put(String.valueOf(i), "@"+Character.toString((char)i));
        }
    }

    @Override
    public List<User> findUsersBySlackNames(List<String> slackNames) {
        for (int i = 0; i < slackNames.size(); i++) {
            if (!slackNames.get(i).startsWith("@")) {
                log.debug("add '@' to SlackName : [{}]", slackNames.get(i));
                String slackName = slackNames.get(i);
                slackNames.set(i, "@" + slackName);
            }
        }
        List<User> users = ALL_USERS.entrySet().stream()
                .filter(user -> slackNames.contains(user.getValue()))
                .map(user -> new User(user.getKey(), user.getValue()))
                .collect(Collectors.toList());
        if (users.size() != slackNames.size()) {
            List<String> absentSlackNames = slackNames.stream()
                    .filter(user -> !ALL_USERS.containsValue(user))
                    .collect(Collectors.toList());
            throwUserException(absentSlackNames);
        }
        log.info("Found '{}' ALL_USERS in Fake repo by slackNames", users.size());
        return users;
    }

    private void throwUserException(List<String> absentUsers) {

        String message = String.format("User(s) '%s' not found",
                absentUsers.stream().collect(Collectors.joining(",")));
        ApiError apiError = new ApiError(
                400, "USF-F1-D1",
                message,
                "User not found",
                "Something went wrong",
                Collections.emptyList()
        );

        throw new UserExchangeException(apiError, new RuntimeException(message));
    }

    @Override
    public List<User> findUsersByUuids(List<String> uuids) {
        List<User> users = ALL_USERS.entrySet().stream()
                .filter(user -> uuids.contains(user.getKey()))
                .map(user -> new User(user.getKey(), user.getValue()))
                .collect(Collectors.toList());
        if (users.size() != uuids.size()) {
            List<String> absentUuids = uuids.stream()
                    .filter(user -> !ALL_USERS.containsKey(user))
                    .collect(Collectors.toList());
            throwUserException(absentUuids);
        }
        log.info("Found User: '{}' by uuids '{}'", users, uuids);
        return users;
    }
}

