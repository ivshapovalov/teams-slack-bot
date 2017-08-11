package ua.com.juja.microservices.teams.slackbot.model;

import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@AllArgsConstructor
public class UserUuidRequest {
    List<String> uuids;

    public List<String> getUuids() {
        return Collections.unmodifiableList(uuids);
    }
}
