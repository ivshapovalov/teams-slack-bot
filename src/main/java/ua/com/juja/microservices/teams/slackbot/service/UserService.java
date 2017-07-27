package ua.com.juja.microservices.teams.slackbot.service;

import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
public interface UserService {

    List<UserDTO> findUsersBySlackNames(List<String> slackNames);

    List<UserDTO> findUsersByUuids(List<String> uuids);
}
