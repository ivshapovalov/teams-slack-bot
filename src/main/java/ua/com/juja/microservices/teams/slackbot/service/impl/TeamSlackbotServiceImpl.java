package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;
import ua.com.juja.microservices.teams.slackbot.service.TeamSlackbotService;

import javax.inject.Inject;
import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
@Slf4j
@Service
public class TeamSlackbotServiceImpl implements TeamSlackbotService {

    private static final int TEAM_SIZE = 4;

    private final SlackNameHandlerService slackNameHandlerService;

    private final TeamService teamService;

    @Inject
    public TeamSlackbotServiceImpl(SlackNameHandlerService slackNameHandlerService, TeamService teamService) {
        this.slackNameHandlerService = slackNameHandlerService;
        this.teamService = teamService;
    }

    @Override
    public Team activateTeam(String text) {

        log.debug("Started extract members from text '{}'", text);
        Set<String> members = slackNameHandlerService.getUuidsFromText(text);
        log.debug("Finished extract members '{}' from text '{}'", members, text);

        if (members.size() != TEAM_SIZE) {
            log.warn("Members size is not equals '{}'" + TEAM_SIZE);
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command." +
                    " But size of the team must be %s.", members.size(), TEAM_SIZE));
        }
        log.debug("Started create TeamRequest");
        TeamRequest teamRequest = new TeamRequest(members);
        log.debug("Finished create TeamRequest");

        log.debug("Send activate team request to Teams service. Team: '{}'", teamRequest.toString());
        Team activatedTeam = teamService.activateTeam(teamRequest);
        log.debug("Received response from Teams service: '{}'", activatedTeam.toString());

        log.info("Team activated: '{}'", activatedTeam.getId());
        return activatedTeam;
    }
}
