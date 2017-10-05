package ua.com.juja.microservices.teams.slackbot.model.teams;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Team {
    @JsonProperty("members")
    private final Set<String> members;

    @JsonProperty("from")
    private String from;

    @Getter
    @JsonProperty("id")
    private String id;

    @JsonProperty("activateDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    private Date activateDate;

    @JsonProperty("deactivateDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    private Date deactivateDate;

    @JsonCreator
    public Team(@JsonProperty("from") String from, @JsonProperty("id") String id,
                @JsonProperty("activateDate") Date activateDate, @JsonProperty("deactivateDate") Date deactivateDate,
                @JsonProperty("members") Set<String> members) {
        this.from = from;
        this.id = id;
        this.activateDate = activateDate;
        this.deactivateDate = deactivateDate;
        this.members = members;
    }

    @JsonCreator
    public Team(@JsonProperty("members") Set<String> members) {
        this.members = members;
    }

    @JsonCreator
    public Team(@JsonProperty("from") String from,
                @JsonProperty("members") Set<String> members) {
        this.from = from;
        this.members = members;
    }

    public Set<String> getMembers() {
        return Collections.unmodifiableSet(members);
    }
}
