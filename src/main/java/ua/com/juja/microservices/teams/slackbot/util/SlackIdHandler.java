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
public class SlackIdHandler {
    /**
     * Slack name cannot be longer than 21 characters and
     * can only contain letters, numbers, periods, hyphens, and underscores.
     * ([a-z0-9\.\_\-]){1,21}
     * quick test regExp http://regexr.com/
     */
    private static final String SLACK_ID_PATTERN = "\\<@(.*?)(\\||\\>)";
    private static final String SLACK_ID_WRAPPER_PATTERN = "<@%s>";

    public static Set<String> getSlackIdsFromText(String text) {
        log.debug("Before extract slackIds from text '{}'", text);
        Set<String> slackIds = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile(SLACK_ID_PATTERN);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            slackIds.add(matcher.group(1));
        }
        log.info("After extract slackIds '{}' from text '{}'", slackIds.toString(), text);
        return slackIds;
    }

    public static String wrapSlackId(String slackId) {
        return String.format(SLACK_ID_WRAPPER_PATTERN, slackId);
    }
}

