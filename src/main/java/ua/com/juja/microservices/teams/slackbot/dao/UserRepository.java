package ua.com.juja.microservices.teams.slackbot.dao;

import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
public interface UserRepository {

    List<UserDTO> findUsersBySlackNames(List<String> slackNames);

    List<UserDTO> findUsersByUuids(List<String> uuids);

}
