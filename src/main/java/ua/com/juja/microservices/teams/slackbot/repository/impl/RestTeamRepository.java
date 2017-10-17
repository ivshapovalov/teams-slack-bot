package ua.com.juja.microservices.teams.slackbot.repository.impl;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
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
public class RestTeamRepository implements TeamRepository {

    @Value("${teams.endpoint.activateTeam}")
    private String teamsActivateTeamUrl;
    @Value("${teams.endpoint.deactivateTeam}")
    private String teamsDeactivateTeamUrl;
    @Value("${teams.endpoint.getTeam}")
    private String teamsGetTeamUrl;

    @Inject
    private GatewayClient gatewayClient;

    @Override
    public Team activateTeam(ActivateTeamRequest activateTeamRequest) {
        Team activatedTeam;
        try {
            log.debug("Send 'Activate team' request '{}' to Teams service to url '{}'", activateTeamRequest, teamsActivateTeamUrl);
            activatedTeam = gatewayClient.activateTeam(activateTeamRequest);
            log.debug("Get 'Activate team' response '{}' from Teams service", activatedTeam);
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new TeamExchangeException(error, ex);
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage().substring(ex.getMessage().indexOf("content:") + 8));
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team activated: '{}'", activatedTeam.getId());
        return activatedTeam;
    }

    @Override
    public Team deactivateTeam(DeactivateTeamRequest deactivateTeamRequest) {
        Team deactivatedTeam;
        try {
            log.debug("Send 'Deactivate team' request to Teams service to url '{}'", teamsDeactivateTeamUrl);
            deactivatedTeam = gatewayClient.deactivateTeam(deactivateTeamRequest);
            log.debug("Get 'Deactivate team' response '{}' from Teams service", deactivatedTeam);
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new TeamExchangeException(error, ex);
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage().substring(ex.getMessage().indexOf("content:") + 8));
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team deactivated: '{}'", deactivatedTeam.getId());
        return deactivatedTeam;
    }

    @Override
    public Team getTeam(String uuid) {
        Team team;
        try {
            log.debug("Send 'Get team' request to Teams service to url '{}'", teamsGetTeamUrl);
            team = gatewayClient.getTeam(uuid);
            log.debug("Get 'Get team' response '{}' from Teams service", team);
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            throw new TeamExchangeException(error, ex);
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage().substring(ex.getMessage().indexOf("content:") + 8));
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team got: '{}'", team.getId());
        return team;
    }
}
