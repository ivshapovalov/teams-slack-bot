package ua.com.juja.microservices.teams.slackbot.dao;

import ua.com.juja.microservices.teams.slackbot.model.UserDTO;

import java.util.List;

public interface UserRepository {
    List<UserDTO> findUsersBySlackNames(List<String> slackNames);
}
