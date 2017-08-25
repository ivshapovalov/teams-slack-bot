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
            String teamsServiceURL = teamsBaseUrl + "/" + teamsRestApiVersion + teamsActivateTeamUrl;
            log.debug("Send request '{}' to Teams service to url '{}'", teamRequest,teamsServiceURL);
            ResponseEntity<Team> response = restTemplate.exchange(teamsServiceURL,
                    HttpMethod.POST, request, Team.class);
            log.debug("Get response '{}' from Teams service", response);
            activatedTeam = response.getBody();
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team activated: '{}'", activatedTeam.getId());
        return activatedTeam;
    }

    @Override
    public Team deactivateTeam(String slackName) {
        //TODO Should be implemented feature SLB-F2
        return null;
    }

    @Override
    public Team getTeam(String uuid) {
        HttpEntity<TeamRequest> request = new HttpEntity<>(Utils.setupJsonHttpHeaders());
        Team team;
        try {
            String teamsServiceURL = teamsBaseUrl + "/" + teamsRestApiVersion + teamsGetTeamUrl + "/" + uuid;
            log.debug("Send request to Teams service to url '{}'", teamsServiceURL);
            ResponseEntity<Team> response = restTemplate.exchange(teamsServiceURL,
                    HttpMethod.GET, request, Team.class);
            log.debug("Get response '{}' from Teams service", response);
            team = response.getBody();
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team got: '{}'", team.getId());
        return team;
    }
}
