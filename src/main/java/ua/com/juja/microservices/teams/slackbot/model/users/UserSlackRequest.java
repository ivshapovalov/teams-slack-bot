package ua.com.juja.microservices.teams.slackbot.model.users;

import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@AllArgsConstructor
public class UserSlackRequest {
    List<String> slackUsers;

    public List<String> getSlackUsers() {
        return Collections.unmodifiableList(slackUsers);
    }
}
