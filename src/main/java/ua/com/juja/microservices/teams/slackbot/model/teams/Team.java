package ua.com.juja.microservices.teams.slackbot.model.teams;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
@ToString
@Slf4j
@AllArgsConstructor
public class Team {
    private final Set<String> members;
    private String from;
    @Getter
    private String id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    private Date activateDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    private Date deactivateDate;

    public Set<String> getMembers() {
        return Collections.unmodifiableSet(members);
    }
}
