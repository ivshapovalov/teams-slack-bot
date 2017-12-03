package ua.com.juja.microservices.teams.slackbot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static ApiError convertToApiError(String message) {
        int contentExists = message.indexOf("content:");
        ApiError apiError = new ApiError(
                500, "BotInternalError",
                "I'm, sorry. I cannot parse api error message from remote service :(",
                "Cannot parse api error message from remote service",
                message, Collections.singletonList(message));
        if (contentExists != -1) {
            String apiMessage = message.substring(contentExists + 8);
            ObjectMapper mapper = new ObjectMapper();
            try {
                apiError = mapper.readValue(apiMessage, ApiError.class);
            } catch (IOException e) {
                apiError = new ApiError(
                        500, "BotInternalError",
                        "I'm, sorry. I cannot parse api error message from remote service :(",
                        "Cannot parse api error message from remote service",
                        e.getMessage(), Collections.singletonList(message)
                );
            }
        }
        return apiError;
    }

    private static String listToStringWithDelimeter(Set<String> list, String delimeter) {
        return list.stream()
                .map(SlackIdHandler::wrapSlackIdInFullPattern)
                .collect(Collectors.joining(delimeter));
    }

    private static String arrayToStringWithDelimeter(String[] array, String delimeter) {
        return Arrays.stream(array).collect(Collectors.joining(delimeter));
    }

    public static Set<String> extractUuidsFromExceptionMessage(String message) {
        String messageDelimeter = "#";
        String[] messageParts = message.split(messageDelimeter);
        Set<String> uuids = Collections.emptySet();
        if (messageParts.length > 1) {
            uuids = new HashSet<>(Arrays.asList(messageParts[1].split(",")));
        }
        return uuids;
    }

    public static String replaceUuidsBySlackIdsInExceptionMessage(String message, Set<String> slackIds) {
        String messageDelimeter = "#";
        String[] messageParts = message.split(messageDelimeter);
        if (messageParts.length > 1) {
            messageParts[1] = Utils.listToStringWithDelimeter(slackIds, ",");
            message = Utils.arrayToStringWithDelimeter(messageParts, "");
        }
        return message;
    }
}
