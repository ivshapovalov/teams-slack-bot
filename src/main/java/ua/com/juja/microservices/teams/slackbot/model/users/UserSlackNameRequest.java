package ua.com.juja.microservices.teams.slackbot.model.users;

import lombok.AllArgsConstructor;
import lombok.ToString;

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
