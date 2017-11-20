package ua.com.juja.microservices.teams.slackbot.repository;

import ua.com.juja.microservices.teams.slackbot.model.teams.ActivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.DeactivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.Team;

/**
 * @author Ivan Shapovalov
 */
public interface TeamRepository {

    Team activateTeam(ActivateTeamRequest activateTeamRequest);

    Team deactivateTeam(DeactivateTeamRequest deactivateTeamRequest);

    Team getTeam(String slackName);

}
