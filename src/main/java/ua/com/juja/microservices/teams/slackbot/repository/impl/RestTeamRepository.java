package ua.com.juja.microservices.teams.slackbot.repository.impl;

import com.netflix.discovery.EurekaClient;
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
import ua.com.juja.microservices.teams.slackbot.model.teams.ActivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.DeactivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.Team;
import ua.com.juja.microservices.teams.slackbot.repository.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;

/**
 * @author Ivan Shapovalov
 */
@Repository
@Slf4j
public class RestTeamRepository extends RestRepository implements TeamRepository {

    private final RestTemplate restTemplate;
    @Value("${teams.endpoint.activateTeam}")
    private String teamsActivateTeamUrl;
    @Value("${teams.endpoint.deactivateTeam}")
    private String teamsDeactivateTeamUrl;
    @Value("${teams.endpoint.getTeam}")
    private String teamsGetTeamUrl;

    @Inject
    public RestTeamRepository(RestTemplate restTemplate, EurekaClient eurekaClient) {
        super(eurekaClient);
        this.restTemplate = restTemplate;
    }

    @Override
    public Team activateTeam(ActivateTeamRequest activateTeamRequest) {
        String fullTeamsActivateTeamUrl = discovery(teamsActivateTeamUrl);
        HttpEntity<ActivateTeamRequest> request = new HttpEntity<>(activateTeamRequest, Utils.setupJsonHttpHeaders());
        Team activatedTeam;
        try {
            log.debug("Send 'Activate team' request '{}' to Teams service to url '{}'", activateTeamRequest, fullTeamsActivateTeamUrl);
            ResponseEntity<Team> response = restTemplate.exchange(fullTeamsActivateTeamUrl,
                    HttpMethod.POST, request, Team.class);
            log.debug("Get 'Activate team' response '{}' from Teams service", response);
            activatedTeam = response.getBody();
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team activated: '{}'", activatedTeam.getId());
        return activatedTeam;
    }

    @Override
    public Team deactivateTeam(DeactivateTeamRequest deactivateTeamRequest) {
        String fullTeamsDeactivateTeamUrl = discovery(teamsDeactivateTeamUrl);
        HttpEntity<DeactivateTeamRequest> request = new HttpEntity<>(deactivateTeamRequest, Utils.setupJsonHttpHeaders());
        Team deactivatedTeam;
        try {
            log.debug("Send 'Deactivate team' request to Teams service to url '{}'", fullTeamsDeactivateTeamUrl);
            ResponseEntity<Team> response = restTemplate.exchange(fullTeamsDeactivateTeamUrl,
                    HttpMethod.PUT, request, Team.class);
            log.debug("Get 'Deactivate team' response '{}' from Teams service", response);
            deactivatedTeam = response.getBody();
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team deactivated: '{}'", deactivatedTeam.getId());
        return deactivatedTeam;
    }

    @Override
    public Team getTeam(String uuid) {
        String fullTeamsGetTeamUrl = discovery(teamsGetTeamUrl) + "/" + uuid;
        HttpEntity<ActivateTeamRequest> request = new HttpEntity<>(Utils.setupJsonHttpHeaders());
        Team team;
        try {
            log.debug("Send 'Get team' request to Teams service to url '{}'", fullTeamsGetTeamUrl);
            ResponseEntity<Team> response = restTemplate.exchange(fullTeamsGetTeamUrl,
                    HttpMethod.GET, request, Team.class);
            log.debug("Get 'Get team' response '{}' from Teams service", response);
            team = response.getBody();
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team got: '{}'", team.getId());
        return team;
    }
}
