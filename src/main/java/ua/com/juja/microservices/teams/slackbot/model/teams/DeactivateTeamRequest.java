package ua.com.juja.microservices.teams.slackbot.model.teams;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Ivan Shapovalov
 */
@ToString
@Slf4j
@Getter
@EqualsAndHashCode
public class DeactivateTeamRequest {
    @NotEmpty
    private final String from;
    @NotEmpty
    private final String uuid;

    public DeactivateTeamRequest(String from, String uuid) {
        this.from = from;
        this.uuid = uuid;
    }
}
