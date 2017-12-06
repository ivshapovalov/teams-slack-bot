package ua.com.juja.microservices.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Ivan Shapovalov
 */
@Slf4j
public class TestUtils {

    public static String getUrlTemplate(String endpoint) {
        return endpoint + "?" +
                "token={slashCommandToken}&" +
                "team_id={team_id}&" +
                "team_domain={team_domain}&" +
                "channel_id={channel_id}&" +
                "channel_name={channel_name}&" +
                "user_id={user_id}&" +
                "user_name={user_name}&" +
                "command={command}&" +
                "text={text}&" +
                "response_url={response_url}&";
    }

    public static Object[] getUriVars(String slackToken, String command, String description, String responseUrl) {
        return new Object[]{slackToken,
                "any_team_id",
                "any_domain",
                "UHASHB8JB",
                "test-channel",
                "slack-from",
                "@slack-from",
                command,
                description,
                responseUrl};
    }
}
