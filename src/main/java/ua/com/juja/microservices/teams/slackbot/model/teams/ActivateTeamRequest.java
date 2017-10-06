package ua.com.juja.microservices.teams.slackbot.model.teams;

import lombok.Getter;
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
@Getter
public class ActivateTeamRequest {
    @NotEmpty
    private String from;
    @NotEmpty
    private final Set<String> members;

    public ActivateTeamRequest(String from, Set<String> members) {
        this.from = from;
        this.members = members;
    }

    public Set<String> getMembers() {
        return Collections.unmodifiableSet(members);
    }
}
