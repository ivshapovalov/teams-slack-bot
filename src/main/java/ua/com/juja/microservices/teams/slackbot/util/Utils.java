package ua.com.juja.microservices.teams.slackbot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.model.Team;

import java.io.IOException;
import java.io.Reader;

/**
 * @author Ivan Shapovalov
 */
@Slf4j
public class Utils {

    public static String convertToString(Reader reader) throws IOException {
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }
        return buffer.toString();
    }

    public static <T> void checkNull(T parameter, String message) {
        if (parameter == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
