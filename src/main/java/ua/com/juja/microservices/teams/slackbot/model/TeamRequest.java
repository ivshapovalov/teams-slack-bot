package ua.com.juja.microservices.teams.slackbot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ivan Shapovalov
 */
@Getter
@ToString (exclude = "TEAM_SIZE")
@Slf4j
public class TeamRequest {
    private static final int TEAM_SIZE = 4;
    @NotEmpty
    private Set<String> members;

    public TeamRequest(SlackParsedCommand parsedCommand, String responceUrl) {
        log.debug("Started creating TeamRequest");
        String fromUuid = parsedCommand.getFromUser().getUuid();
        log.debug("Map UserDTO to uuid");
        Set<String> members = parsedCommand.getUsers()
                .stream().map(user -> user.getUuid())
                .collect(Collectors.toSet());
        if (members.size() != TEAM_SIZE) {
            members.remove(fromUuid);
        }
        if (members.size() == 0) {
            log.warn("Members size is equals 0");
            throw new WrongCommandFormatException(String.format("We didn't find slack name in your command. '%s'" +
                            " You must write %s user's slack names for activate team.", parsedCommand.getText(),
                    TEAM_SIZE));
        } else if (members.size() != TEAM_SIZE) {
            log.warn("Members size is not equals '{}'" + TEAM_SIZE);
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command: '%s' " +
                            " But size of the team must be %s.", members.size(),
                    parsedCommand.getText(), TEAM_SIZE));
        }

        this.members = members;
        log.debug("Finished creating new TeamRequest '{}'",this);
        log.info("Finished creating new TeamRequest");
    }
}
