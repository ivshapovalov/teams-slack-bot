package ua.com.juja.microservices.teams.slackbot.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        log.debug("Before extract slackNames from text '{}", text);
        List<String> slackNames = new ArrayList<>();
        Pattern pattern = Pattern.compile(SLACK_NAME_PATTERN);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            slackNames.add(matcher.group());
        }
        log.info("After extract slackNames '{}' from text '{}", slackNames.toString(), text);
        return slackNames;
    }

    public static void addAtToSlackNames(List<String> slackNames) {
        for (int i = 0; i < slackNames.size(); i++) {
            if (!slackNames.get(i).startsWith("@")) {
                String slackName = slackNames.get(i);
                slackNames.set(i, "@" + slackName);
            }
        }
    }
}

