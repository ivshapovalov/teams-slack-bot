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
public class FakeUserRepository extends AbstractRestRepository implements UserRepository {

    private static final Map<String, String> ALL_USERS = new HashMap<>();

    static {
        ALL_USERS.put("1", "@a");
        ALL_USERS.put("2", "@b");
        ALL_USERS.put("3", "@c");
        ALL_USERS.put("4", "@d");
        ALL_USERS.put("5", "@e");
        ALL_USERS.put("6", "@f");
        ALL_USERS.put("7", "@g");
        ALL_USERS.put("8", "@h");
        ALL_USERS.put("9", "@i");
        ALL_USERS.put("10", "@j");
        ALL_USERS.put("11", "@k");
        ALL_USERS.put("12", "@l");
    }

    @Override
    public List<User> findUsersBySlackNames(List<String> slackNames) {
        log.debug("Received slackNames to search : '{}' in Fake repo", slackNames);
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
        log.debug("Finished searching in Fake Users service. Users is: [{}]", users.toString());
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
        log.debug("Received uuids to convert : '{}'", uuids);
        List<User> users = FakeUserRepository.ALL_USERS.entrySet().stream()
                .filter(user -> uuids.contains(user.getKey()))
                .map(user -> new User(user.getKey(), user.getValue()))
                .collect(Collectors.toList());
        if (FakeUserRepository.ALL_USERS.size() != uuids.size()) {
            List<String> absentUuids = uuids.stream()
                    .filter(user -> !ALL_USERS.containsKey(user))
                    .collect(Collectors.toList());
            throwUserException(absentUuids);
        }
        log.debug("Finished request to Fake Users service. Response is: '{}'", users.toString());
        log.info("Found User: '{}' by uuids '{}'", users, uuids);
        return users;
    }
}
