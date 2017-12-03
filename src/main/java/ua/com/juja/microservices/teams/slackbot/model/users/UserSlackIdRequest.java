package ua.com.juja.microservices.teams.slackbot.model.users;

import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@AllArgsConstructor
public class UserSlackIdRequest {
    List<String> slackIds;

    public List<String> getSlackIds() {
        return Collections.unmodifiableList(slackIds);
    }
}
