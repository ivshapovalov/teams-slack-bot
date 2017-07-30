package ua.com.juja.microservices.teams.slackbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * @author Nikolay Horushko
 */
@AllArgsConstructor
public class SlackNameRequest {
    List<String> slackNames;

    public List<String> getSlackNames() {
        return Collections.unmodifiableList(slackNames);
    }
}
