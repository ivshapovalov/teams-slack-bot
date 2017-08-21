package ua.com.juja.microservices.teams.slackbot.service;

import ua.com.juja.microservices.teams.slackbot.model.Team;

import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
public interface TeamService {

    Team activateTeam(String text);

    Set<String> getTeam(String text);

}
