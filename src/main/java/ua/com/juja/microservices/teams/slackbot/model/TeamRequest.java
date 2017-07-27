package ua.com.juja.microservices.teams.slackbot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;
import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Andrii.Sidun
 * @author Ivan Shapovalov
 */
@Getter
@ToString
@Slf4j
public class TeamRequest {

    private static final int TEAM_MAX_SIZE = 4;

    @JsonProperty("from")
    private String fromUuid;

    @NotEmpty
    private Set<String> members;

    @JsonCreator
    public TeamRequest(@JsonProperty("members") Set<String> members) {
        this.members = members;
    }

    public TeamRequest(SlackParsedCommand parsedCommand) {
        log.debug("Started creating TeamRequest");
        this.fromUuid = parsedCommand.getFromUser().getUuid();
        log.debug("Map UserDTO to uuid");
        Set<String> members = parsedCommand.getTeamMembers()
                .stream().map(user -> user.getUuid())
                .collect(Collectors.toSet());
        if (members.size() > TEAM_MAX_SIZE) {
            log.warn("Members size is greater then {}" + TEAM_MAX_SIZE);
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command: '%s' " +
                            " Max size of the team is %s.", members.size(),
                    parsedCommand.getText(), TEAM_MAX_SIZE));
        } else if (members.size() == 0) {
            log.warn("Members size is equals 0");
            throw new WrongCommandFormatException(String.format("We didn't find slack name in your command. '%s'" +
                            " You must write %s user's slack name for actibvate team.", parsedCommand.getText(),
                    TEAM_MAX_SIZE));
        }
        this.members = members;
        log.debug("Finished creating new TeamRequest");
    }

    private UserDTO receiveToUser(SlackParsedCommand slackParsedCommand) {
        if (slackParsedCommand.getUserCountInText() > TEAM_MAX_SIZE) {
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command: '%s' " +
                            " Max size of the team is %s.", slackParsedCommand.getUserCountInText(),
                    slackParsedCommand.getText(), TEAM_MAX_SIZE));
        }
        if (slackParsedCommand.getUserCountInText() == 0) {
            throw new WrongCommandFormatException(String.format("We didn't find slack name in your command. '%s'" +
                            " You must write %s user's slack name for actibvate team.", slackParsedCommand.getText(),
                    TEAM_MAX_SIZE));
        }
        return slackParsedCommand.getFirstUser();
    }
}
