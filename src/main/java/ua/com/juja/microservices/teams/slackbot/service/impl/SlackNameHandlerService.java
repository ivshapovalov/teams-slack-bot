package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;
import ua.com.juja.microservices.teams.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.teams.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
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
            log.debug("Add '@' to slack name: '{}'", fromSlackName);
            fromSlackName = "@" + fromSlackName;
        }
        log.debug("Started creating users map for user '{}' and text '{}'", fromSlackName, text);
        List<String> slackNames = extractSlackNamesFromText(text);
        slackNames.add(fromSlackName);
        log.debug("Started finding slack names: '{}' in User service", slackNames);
        Set<UserDTO> users = new HashSet<>(userService.findUsersBySlackNames(slackNames));
        log.debug("Finished finding slack names: '{}' in User service", users.toString());
        log.debug("Started create slackParsedCommand from user '{}': text '{}", fromSlackName, text);
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromSlackName, text, users);
        log.debug("Finished create slackParsedCommand by user '{}' from text '{}'", fromSlackName, text);
        log.info("Create slackParsedCommand by user '{}' from text '{}", fromSlackName, text);
        return slackParsedCommand;
    }

    public Set<String> convertUuidsToSlackNames(Set<String> members) {
        log.debug("Started find uuids '{}' in User service", members);
        List<UserDTO> userDTOs = userService.findUsersByUuids(new ArrayList<>(members));
        log.debug("Finished find uuids '{}' in User service", userDTOs.toString());
        log.debug("Start Convert usersDTO '{}' to set", userDTOs.toString());
        Set<String> slackNames = userDTOs.stream().map(user -> user.getSlack())
                .collect(Collectors.toSet());
        log.debug("Finished Convert usersDTO '{}' to set '{}'", userDTOs.toString(),slackNames.toString());
        log.info("Created users set '{}'", slackNames.toString());
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

