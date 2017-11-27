package ua.com.juja.microservices.teams.slackbot.service;

import ua.com.juja.microservices.teams.slackbot.model.users.User;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
public interface UserService {

    String replaceUuidsBySlackIdsInExceptionMessage(String message);

    List<User> findUsersBySlackIds(List<String> slackIds);

    List<User> findUsersByUuids(List<String> uuids);

}
