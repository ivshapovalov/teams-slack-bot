package ua.com.juja.microservices.teams.slackbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@AllArgsConstructor
public class UserSlackNameRequest {
    List<String> slackNames;

    public List<String> getSlackNames() {
        return Collections.unmodifiableList(slackNames);
    }
}
