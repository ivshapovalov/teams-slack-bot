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
                    "I'm, sorry. I cannot parse api error message from remote service :(",
                    "Cannot parse api error message from remote service",
                    e.getMessage(),
                    Collections.singletonList(httpClientErrorException.getMessage())
            );
        }
    }

    public static String listToStringWithDelimeter(Set<String> list, String delimeter) {
        return list.stream().collect(Collectors.joining(delimeter));
    }

    public static String arrayToStringWithDelimeter(String[] array, String delimeter) {
        return Arrays.stream(array).collect(Collectors.joining(delimeter));
    }
}
