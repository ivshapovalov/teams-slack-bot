package ua.com.juja.microservices.teams.slackbot.service;

import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
public interface TeamService {

    Set<String> activateTeam(String fromSlackUser, String text);

    Set<String> getTeam(String text);

    Set<String> deactivateTeam(String fromSlackUser, String text);
}
