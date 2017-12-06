package ua.com.juja.microservices.teams.slackbot.util;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivan Shapovalov
 */
@Slf4j
public class SlackUserHandler {
    private static final String SLACK_USER_PATTERN = "\\<@(.*?)(\\||\\>)";
    private static final String SLACK_USER_WRAPPER_FULL_PATTERN = "<@%s>";
    private static final String SLACK_USER_WRAPPER_PARTIAL_PATTERN = "<@%s|";

    public static Set<String> getSlackUsersFromText(String text) {
        log.debug("Before extract slack Users from text '{}'", text);
        Set<String> slackUsers = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile(SLACK_USER_PATTERN);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            slackUsers.add(matcher.group(1));
        }
        log.info("After extract slack Users '{}' from text '{}'", slackUsers.toString(), text);
        return slackUsers;
    }

    public static String wrapSlackUserInFullPattern(String slackUser) {
        return String.format(SLACK_USER_WRAPPER_FULL_PATTERN, slackUser);
    }

    public static String wrapSlackUserInPartialPattern(String slackUser) {
        return String.format(SLACK_USER_WRAPPER_PARTIAL_PATTERN, slackUser);
    }
}

