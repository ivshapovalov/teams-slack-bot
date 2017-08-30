package ua.com.juja.microservices.teams.slackbot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
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

    public static HttpHeaders setupJsonHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return headers;
    }

    public static ApiError convertToApiError(HttpClientErrorException httpClientErrorException) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(httpClientErrorException.getResponseBodyAsString(), ApiError.class);
        } catch (IOException e) {
            return new ApiError(
                    500, "BotInternalError",
                    "I'm, sorry. I cannot parse api error messages from remote service :(",
                    "Cannot parse api error messages from remote service",
                    e.getMessage(),
                    Collections.singletonList(httpClientErrorException.getMessage())
            );
        }
    }

    private static String listToStringWithDelimeter(Set<String> list, String delimeter) {
        return list.stream().collect(Collectors.joining(delimeter));
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

    public static String replaceUuidsBySlackNamesInExceptionMessage(String message, Set<String> slackNames) {
        String messageDelimeter = "#";
        String[] messageParts = message.split(messageDelimeter);
        if (messageParts.length > 1) {
            messageParts[1] = Utils.listToStringWithDelimeter(slackNames, ",");
            message = Utils.arrayToStringWithDelimeter(messageParts, "");
        }
        return message;
    }
}
