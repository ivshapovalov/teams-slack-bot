package ua.com.juja.microservices.teams.slackbot.model;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;
import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Nikolay Horushko
 * @author Ivan Shapovalov
 */
@ToString
@Slf4j
public class SlackParsedCommand {
    private String fromSlackName;
    private String text;
    private Map<String, UserDTO> users;

    public SlackParsedCommand(String fromSlackName, String text, Map<String, UserDTO> users) {
        this.fromSlackName = fromSlackName;
        this.text = text;
        this.users = users;
    }

    public String getText() {
        return text;
    }

    public UserDTO getFromUser() {
        return users.get(fromSlackName);
    }

    public Set<UserDTO> getUsers() {
        checkIsTextContainsSlackName();
        Set<UserDTO> result = new LinkedHashSet<>(users.values());
        log.debug("Found '{}' team members in the text: '{}'", result.size(), text);
        return result;
    }

    public void checkIsTextContainsSlackName() {
        if (users.size() == 0) {
            log.warn("The text: '{}' doesn't contain slack name.");
            throw new WrongCommandFormatException(String.format("The text '%s' doesn't contains slackName", text));
        }
    }
}
