package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.model.UserDTO;
import ua.com.juja.microservices.teams.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    public Set<String> getUuidsFromText(String text) {
        log.debug("Started creating users set for text '{}'", text);
        List<String> slackNames = getSlackNamesFromText(text);
        log.debug("Finished creating users set for text '{}'. Set is '{}'", text, slackNames);
        log.debug("Started finding slack names: '{}' in User service", slackNames);
        Set<UserDTO> usersDTO = new HashSet<>(userService.findUsersBySlackNames(slackNames));
        log.debug("Finished finding slack names: '{}' in User service", usersDTO.toString());
        Set<String> users = usersDTO.stream()
                .map(user -> user.getUuid())
                .collect(Collectors.toSet());
        log.info("Create users set '{}' from text '{}", slackNames, text);
        return users;
    }

    public Set<String> convertUuidsToSlackNames(Set<String> members) {
        log.debug("Started find uuids '{}' in User service", members);
        List<UserDTO> userDTOs = userService.findUsersByUuids(new ArrayList<>(members));
        log.debug("Finished find uuids '{}' in User service", userDTOs.toString());
        log.debug("Start Convert usersDTO '{}' to set", userDTOs.toString());
        Set<String> slackNames = userDTOs.stream().map(user -> user.getSlack())
                .collect(Collectors.toSet());
        log.debug("Finished Convert usersDTO '{}' to set '{}'", userDTOs.toString(), slackNames.toString());
        log.info("Created users set '{}'", slackNames.toString());
        return slackNames;
    }

    private List<String> getSlackNamesFromText(String text) {
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

