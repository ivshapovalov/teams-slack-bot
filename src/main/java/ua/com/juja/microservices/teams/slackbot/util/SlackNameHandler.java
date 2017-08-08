package ua.com.juja.microservices.teams.slackbot.util;

import lombok.extern.slf4j.Slf4j;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.service.UserService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Ivan Shapovalov
 */
@Slf4j
public class SlackNameHandler {

    /**
     * Slack name cannot be longer than 21 characters and
     * can only contain letters, numbers, periods, hyphens, and underscores.
     * ([a-z0-9\.\_\-]){1,21}
     * quick test regExp http://regexr.com/
     */
    private static final String SLACK_NAME_PATTERN = "@([a-zA-z0-9\\.\\_\\-]){1,21}";

    public static List<String> getSlackNamesFromText(String text) {
        log.debug("Started extract slackNames from text '{}'", text);
        List<String> slackNames = new ArrayList<>();
        Pattern pattern = Pattern.compile(SLACK_NAME_PATTERN);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            slackNames.add(matcher.group());
        }
        log.info("Extracted slackNames '{}' from text '{}", slackNames.toString(), text);
        return slackNames;
    }

    public static void addAtToSlackNames(List<String> slackNames) {
        for (int i = 0; i < slackNames.size(); i++) {
            if (!slackNames.get(i).startsWith("@")) {
                log.debug("Add '@' to slackName : '{}'", slackNames.get(i));
                String slackName = slackNames.get(i);
                slackNames.set(i, "@" + slackName);
            }
        }
    }

    public static String replaceUuidsBySlackNamesInMessage(UserService userService, String message) {
        log.debug("Start find and replace uuids by slackNames in message {}", message);
        String messageDelimeter = "#";
        String[] messageParts = message.split(messageDelimeter);
        if (messageParts.length > 1) {
            Set<String> uuids = new HashSet<>(Arrays.asList(messageParts[1].split(",")));
            Set<String> slackNames = new HashSet<>();
            try {
                List<User> users = userService.findUsersByUuids(new ArrayList<>(uuids));
                slackNames = users.stream().map(User::getSlack)
                        .collect(Collectors.toSet());
            } catch (Exception ex) {
                log.warn("Nested exception : '{}'", ex.getMessage());
            }
            messageParts[1] = Utils.listToStringWithDelimeter(slackNames, ",");
            message = Utils.arrayToStringWithDelimeter(messageParts, "");
        }
        log.debug("Finished find and replace uuids by slackNames in message {}", message);
        return message;
    }
}

