package ua.com.juja.microservices.teams.slackbot.service;

import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.model.Team;

/**
 * @author Ivan Shapovalov
 */
@Service
public interface TeamSlackbotService {

    Team activateTeam(String text);

}
