package ua.com.juja.microservices.teams.slackbot.dao;

import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;

import java.util.List;

public interface TeamRepository {

    Team activateTeam(TeamRequest teamRequest);

    Team deactivateTeam(String slackName);

    Team getTeam(String slackName);

}
