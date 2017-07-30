package ua.com.juja.microservices.teams.slackbot.model;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;

import java.util.Collections;
import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
@ToString(exclude = "TEAM_SIZE")
@Slf4j
public class TeamRequest {
    private static final int TEAM_SIZE = 4;
    @NotEmpty
    private Set<String> members;

    public TeamRequest(Set<String> members) {
        log.debug("Started creating TeamRequest");
        if (members.size() == 0) {
            log.warn("Members size is equals 0");
            throw new WrongCommandFormatException(String.format("We didn't find slack name in your command." +
                    " You must write %s user's slack names for activate team.", TEAM_SIZE));
        } else if (members.size() != TEAM_SIZE) {
            log.warn("Members size is not equals '{}'" + TEAM_SIZE);
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command." +
                    " But size of the team must be %s.", members.size(), TEAM_SIZE));
        }
        this.members = members;
        log.debug("Finished creating new TeamRequest '{}'", this);
        log.info("Finished creating new TeamRequest");
    }

    public Set<String> getMembers() {
        return Collections.unmodifiableSet(members);
    }
}
