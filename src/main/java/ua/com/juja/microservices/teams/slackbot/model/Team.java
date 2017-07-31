package ua.com.juja.microservices.teams.slackbot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
@Data
@ToString
@Slf4j
public class Team {

    @JsonProperty("id")
    private String id;
    @JsonProperty("members")
    private Set<String> members;

    @JsonProperty("activateDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    private Date activateDate;

    @JsonProperty("deactivateDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    private Date deactivateDate;

    @JsonCreator
    public Team(@JsonProperty("members") Set<String> members) {
        this.members = members;
    }

    public Set<String> getMembers() {
        return Collections.unmodifiableSet(members);
    }
}
