package ua.com.juja.microservices.teams.slackbot.service;

import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;

/**
 * @author Ivan Shapovalov
 */
@Service
public interface TeamSlackbotService {

    Team activateTeam(TeamRequest teamRequest);

}
