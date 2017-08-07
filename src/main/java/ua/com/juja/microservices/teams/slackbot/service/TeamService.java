package ua.com.juja.microservices.teams.slackbot.service;

import ua.com.juja.microservices.teams.slackbot.model.Team;

/**
 * @author Ivan Shapovalov
 */
public interface TeamService {

    Team activateTeam(String text);

}
