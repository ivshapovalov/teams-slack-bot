package ua.com.juja.microservices.teams.slackbot.repository;

import ua.com.juja.microservices.teams.slackbot.model.users.User;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
public interface UserRepository {

    List<User> findUsersBySlackNames(List<String> slackNames);

    List<User> findUsersByUuids(List<String> uuids);
}
