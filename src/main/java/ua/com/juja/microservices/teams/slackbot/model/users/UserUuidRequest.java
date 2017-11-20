package ua.com.juja.microservices.teams.slackbot.model.users;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@AllArgsConstructor
@ToString
public class UserUuidRequest {
    List<String> uuids;

    public List<String> getUuids() {
        return Collections.unmodifiableList(uuids);
    }
}
