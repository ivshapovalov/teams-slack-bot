package ua.com.juja.microservices.teams.slackbot.model;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Collections;
import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
@ToString
@Slf4j
public class TeamRequest {

    @NotEmpty
    private final Set<String> members;

    public TeamRequest(Set<String> members) {
        this.members = members;
    }

    public Set<String> getMembers() {
        return Collections.unmodifiableSet(members);
    }
}
