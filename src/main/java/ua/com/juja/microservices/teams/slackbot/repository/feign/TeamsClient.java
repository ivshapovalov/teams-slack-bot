package ua.com.juja.microservices.teams.slackbot.repository.feign;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ua.com.juja.microservices.teams.slackbot.model.teams.ActivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.DeactivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.Team;

/**
 * @author Ivan Shapovalov
 */
@FeignClient(name = "gateway")
public interface TeamsClient {
    @RequestMapping(method = RequestMethod.POST, value = "/v1/teams", consumes =
            MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    Team activateTeam(ActivateTeamRequest request);

    @RequestMapping(method = RequestMethod.PUT, value = "/v1/teams", consumes =
            MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    Team deactivateTeam(DeactivateTeamRequest deactivateTeamRequest);

    @RequestMapping(method = RequestMethod.GET, value = "/v1/teams/users/{uuid}", consumes =
            MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    Team getTeam(@RequestParam(value = "uuid") String uuid);
}
