package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;
import ua.com.juja.microservices.teams.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Ivan Shapovalov
 */

@Service
@Slf4j
public class SlackNameHandlerService {

    /**
     * Slack name cannot be longer than 21 characters and
     * can only contain letters, numbers, periods, hyphens, and underscores.
     * ([a-z0-9\.\_\-]){1,21}
     * quick test regExp http://regexr.com/
     */
    private final String SLACK_NAME_PATTERN = "@([a-zA-z0-9\\.\\_\\-]){1,21}";
    private UserService userService;

    @Inject
    public SlackNameHandlerService(UserService userService) {
        this.userService = userService;
    }

    public SlackParsedCommand createSlackParsedCommand(String fromSlackName, String text) {
        if (!fromSlackName.startsWith("@")) {
            log.debug("Add '@' to slack name: [{}]", fromSlackName);
            fromSlackName = "@" + fromSlackName;
        }
        log.debug("Started create users map for'{}': text '{}", fromSlackName, text);
        Map<String, UserDTO> users = createUsersMapFromSlackNames(fromSlackName, text);
        log.debug("Finished create users map for'{}': text '{}", fromSlackName, text);
        log.debug("Started create slackParsedCommand from user '{}': text '{}", fromSlackName, text);
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromSlackName, text, users);
        log.debug("Finished create slackParsedCommand by user [{}] from text '[{}]", fromSlackName, text);
        return slackParsedCommand;
    }

    private Map<String, UserDTO> createUsersMapFromSlackNames(String fromSlackName, String text) {
        log.debug("Started creating users map for user '{}' and text '{}'", fromSlackName, text);
        List<String> slackNamesInText = extractSlackNamesFromText(text);
        slackNamesInText.add(fromSlackName);
        log.debug("Started find slack names: '{}' in User service", slackNamesInText);
        List<UserDTO> userDTOs = userService.findUsersBySlackNames(slackNamesInText);
        log.debug("Finished find slack names: '{}' in User service", userDTOs.toString());
        log.debug("Convert users '{}' to map", userDTOs.toString());
        Map<String, UserDTO> users = userDTOs.stream()
                .collect(Collectors.toMap(userDTO -> userDTO.getSlack(), user -> user));
        log.info("Create users map '{}'", users.toString());
        return users;
    }

    public Set<String> createUsersSetFromUuids(Set<String> members) {
        log.debug("Started creating users slackName list for team members '{} ", members);
        log.debug("Started find uuids '{}' in User service", members);
        List<UserDTO> userDTOs = userService.findUsersByUuids(new ArrayList<>(members));
        log.debug("Finished find uuids '{}' in User service", userDTOs.toString());
        log.debug("Convert usersDTO '{}' to set", userDTOs.toString());
        Set<String> slackNames = userDTOs.stream().map(user -> user.getSlack())
                .collect(Collectors.toSet());
        log.info("Create users set '{}'", slackNames.toString());
        return slackNames;
    }

    private List<String> extractSlackNamesFromText(String text) {
        log.debug("Started extract slackNames from text '{}'", text);
        List<String> slackNames = new ArrayList<>();
        Pattern pattern = Pattern.compile(SLACK_NAME_PATTERN);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            slackNames.add(matcher.group());
        }
        log.debug("Finished extract slackNames '{}' from text '{}", slackNames.toString(), text);
        log.info("Extracted slackNames '{}' from text '{}", slackNames.toString(), text);
        return slackNames;
    }

}

