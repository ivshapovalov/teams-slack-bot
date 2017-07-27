package ua.com.juja.microservices.teams.slackbot.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.dao.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;

import javax.inject.Inject;

@Repository
@Slf4j
public class RestTeamRepository extends AbstractRestRepository implements TeamRepository {

    private final RestTemplate restTemplate;
    @Value("${rest.api.version}")
    private String restApiVersion;
    @Value("${teams.baseURL}")
    private String urlBase;
    @Value("${endpoint.activateTeam}")
    private String urlActivateTeam;
    @Value("${endpoint.deactivateTeam}")
    private String urlDeactivateTeam;
    @Value("${endpoint.getTeam}")
    private String urlGetTeam;

    @Inject
    public RestTeamRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Team activateTeam(TeamRequest teamRequest) {
        log.debug("Started Activate Team from request: '{}'", teamRequest.toString());
        HttpEntity<TeamRequest> request = new HttpEntity<>(teamRequest, setupBaseHttpHeaders());
        Team activatedTeam;
        try {
            String teamsServiceURL = urlBase + restApiVersion + urlActivateTeam;
            log.debug("Started request to Teams service url '{}'. Request is : '{}'", teamsServiceURL, request
                    .toString());
            ResponseEntity<Team> response = restTemplate.exchange(teamsServiceURL,
                    HttpMethod.POST, request, Team.class);
            log.debug("Finished request to Teams service. Response is: '{}'", response.toString());
            activatedTeam = response.getBody();
        } catch (HttpClientErrorException ex) {
            ApiError error = convertToApiError(ex);
            log.warn("Teams service returned an error: '{}'", error);
            //TODO get users from error
            throw new TeamExchangeException(error, ex);
        }
        log.info("Team activated: '{}'", activatedTeam.toString());
        return activatedTeam;
    }

    @Override
    public Team deactivateTeam(String slackName) {
        return null;
    }

    @Override
    public Team getTeam(String slackName) {
        return null;
    }
}
