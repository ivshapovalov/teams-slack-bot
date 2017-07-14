package ua.com.juja.microservices.teams.slackbot.dao;

import ua.com.juja.microservices.teams.slackbot.model.TeamDTO;

import java.util.List;

public interface TeamRepository {

    String activateTeam(List<String> slackNames);

    String deactivateTeam(String slackName);

    TeamDTO getTeam(String slackName);

}
