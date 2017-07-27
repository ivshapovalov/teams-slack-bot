package ua.com.juja.microservices.teams.slackbot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
@Data
@ToString
@Slf4j
public class Team {

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
        this.activateDate = Date.from(LocalDateTime.of(LocalDate.now(),
                LocalTime.MIN).atZone(ZoneId.systemDefault()).toInstant());
        this.deactivateDate = Date.from(LocalDateTime.of(LocalDate.now().plusMonths(1).minusDays(1),
                LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
    }
}
