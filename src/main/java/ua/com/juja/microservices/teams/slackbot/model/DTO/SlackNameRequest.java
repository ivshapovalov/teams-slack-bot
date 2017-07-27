package ua.com.juja.microservices.teams.slackbot.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @author Nikolay Horushko
 */
@Getter
@AllArgsConstructor
public class SlackNameRequest {
    List<String> slackNames;
}
