package ua.com.juja.microservices.teams.slackbot.service;

import ua.com.juja.microservices.teams.slackbot.model.users.User;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
public interface UserService {

    String replaceUuidsBySlackUsersInExceptionMessage(String message);

    List<User> findUsersBySlackUsers(List<String> slackUsers);

    List<User> findUsersByUuids(List<String> uuids);

}
