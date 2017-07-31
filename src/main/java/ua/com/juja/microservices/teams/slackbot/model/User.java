package ua.com.juja.microservices.teams.slackbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Nikolay Horushko
 */
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class User {
    @JsonProperty
    String uuid;
    @JsonProperty
    String slack;
}
