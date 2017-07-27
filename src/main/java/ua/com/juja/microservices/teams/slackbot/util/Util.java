package ua.com.juja.microservices.teams.slackbot.util;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ivan Shapovalov
 */
public class Util {

    public static void sendResultRichMessage(String responseUrl, RichMessage richMessage){
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(responseUrl, richMessage, String.class);
    }
}
