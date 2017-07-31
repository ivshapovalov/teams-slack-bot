package ua.com.juja.microservices.teams.slackbot.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Ivan Shapovalov
 */
@Slf4j
public class Utils {

    public static <T> void checkNull(T parameter, String message) {
        if (parameter == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
