package ua.com.juja.microservices.teams.slackbot.repository;

import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;

/**
 * @author Ivan Shapovalov
 */
public interface TeamRepository {

    Team activateTeam(TeamRequest teamRequest);

    Team deactivateTeam(String slackName);

    Team getTeam(String slackName);

}
