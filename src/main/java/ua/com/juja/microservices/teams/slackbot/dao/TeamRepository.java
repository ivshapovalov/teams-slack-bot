package ua.com.juja.microservices.teams.slackbot.dao;

import ua.com.juja.microservices.teams.slackbot.model.Team;

import java.util.List;

public interface TeamRepository {

    Team activateTeam(List<String> slackNames);

    Team deactivateTeam(String slackName);

    Team getTeam(String slackName);

}
