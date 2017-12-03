package ua.com.juja.microservices.teams.slackbot.repository;

import ua.com.juja.microservices.teams.slackbot.model.users.User;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
public interface UserRepository {

    List<User> findUsersBySlackIds(List<String> slackIds);

    List<User> findUsersByUuids(List<String> uuids);
}
