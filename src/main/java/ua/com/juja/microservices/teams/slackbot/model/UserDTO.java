package ua.com.juja.microservices.teams.slackbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class UserDTO {
    @JsonProperty
    private String uuid;
    @JsonProperty
    private String slack;
}
