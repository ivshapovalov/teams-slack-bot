package ua.com.juja.microservices.teams.slackbot.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.repository.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
@Repository
@Slf4j
public class RestTeamRepository implements TeamRepository {

    private final RestTemplate restTemplate;
    @Value("${teams.rest.api.version}")
    private String teamsRestApiVersion;
    @Value("${teams.baseURL}")
    private String teamsBaseUrl;
    @Value("${teams.endpoint.activateTeam}")
    private String teamsActivateTeamUrl;
    @Value("${teams.endpoint.deactivateTeam}")
    private String teamsDeactivateTeamUrl;
    @Value("${teams.endpoint.getTeam}")
    private String teamsGetTeamUrl;

    @Inject
    public RestTeamRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Team activateTeam(TeamRequest teamRequest) {
        HttpEntity<TeamRequest> request = new HttpEntity<>(teamRequest, Utils.setupJsonHttpHeaders());
        Team activatedTeam;
        try {
            String teamsServiceURL = teamsBaseUrl + teamsRestApiVersion + teamsActivateTeamUrl;
            ResponseEntity<Team> response = restTemplate.exchange(teamsServiceURL,
                    HttpMethod.POST, request, Team.class);
            activatedTeam = response.getBody();
            checkTeamMembersEquality(teamRequest.getMembers(),activatedTeam.getMembers());
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team activated: '{}'", activatedTeam.getId());
        return activatedTeam;
    }

    private void checkTeamMembersEquality(Set<String> requestMembers,Set<String> responseMembers) {
        if (!(requestMembers.containsAll(responseMembers)&&responseMembers.containsAll(requestMembers))) {
            Exception ex=new Exception("Team members is not equals in request and response from Teams Service");
            ApiError apiError=new ApiError(
                    500, "BotInternalError",
                    ex.getMessage(),
                    ex.getMessage(),
                    ex.getMessage(),
                    Collections.singletonList("")
            );
            throw new TeamExchangeException(apiError,ex);
        }
    }


    @Override
    public Team deactivateTeam(String slackName) {
        //TODO Should be implemented feature SLB-F2
        return null;
    }

    @Override
    public Team getTeam(String slackName) {
        //TODO Should be implemented feature SLB-F3
        return null;
    }
}
