package ua.com.juja.microservices.teams.slackbot.service;

import ua.com.juja.microservices.teams.slackbot.model.teams.Team;

import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
public interface TeamService {

    Team activateTeam(String fromUser, String text);

    Set<String> getTeam(String text);

    Set<String> deactivateTeam(String fromUser, String text);
}
