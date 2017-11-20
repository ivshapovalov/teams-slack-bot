package ua.com.juja.microservices.teams.slackbot.service;

import ua.com.juja.microservices.teams.slackbot.model.users.User;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
public interface UserService {

    String replaceUuidsBySlackNamesInExceptionMessage(String message);

    List<User> findUsersBySlackNames(List<String> slackNames);

    List<User> findUsersByUuids(List<String> uuids);

}
