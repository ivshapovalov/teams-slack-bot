package ua.com.juja.microservices.teams.slackbot.service;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.stereotype.Service;

/**
 * @author Ivan Shapovalov
 */
@Service
public interface TeamSlackbotService {

    RichMessage activateTeam(String text);

}
