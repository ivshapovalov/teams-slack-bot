package ua.com.juja.microservices.teams.slackbot.model.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Ivan Shapovalov
 */
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class User {
    @JsonProperty
    private String uuid;
    @JsonProperty
    private String slackId;
}
