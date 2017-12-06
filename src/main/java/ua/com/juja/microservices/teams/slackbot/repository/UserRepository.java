package ua.com.juja.microservices.teams.slackbot.repository;

import ua.com.juja.microservices.teams.slackbot.model.users.User;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
public interface UserRepository {

    List<User> findUsersBySlackUsers(List<String> slackUsers);

    List<User> findUsersByUuids(List<String> uuids);
}
