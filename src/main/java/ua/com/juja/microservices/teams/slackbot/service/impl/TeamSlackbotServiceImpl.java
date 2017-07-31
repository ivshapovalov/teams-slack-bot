package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;
import ua.com.juja.microservices.teams.slackbot.service.TeamSlackbotService;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
@Slf4j
@Service
public class TeamSlackbotServiceImpl implements TeamSlackbotService {

    private final SlackNameHandlerService slackNameHandlerService;

    private final TeamService teamService;

    @Inject
    public TeamSlackbotServiceImpl(SlackNameHandlerService slackNameHandlerService, TeamService teamService) {
        this.slackNameHandlerService = slackNameHandlerService;
        this.teamService = teamService;
    }

    @Override
    public RichMessage activateTeam(String text) {

        log.debug("Started extract members from text '{}'", text);
        Set<String> members = slackNameHandlerService.getUuidsFromText(text);
        log.debug("Finished extract members '{}' from text '{}'", members,text);

        log.debug("Started create TeamRequest");
        TeamRequest teamRequest = new TeamRequest(members);
        log.debug("Finished create TeamRequest");

        log.debug("Send activate team request to Teams service. Team: '{}'", teamRequest.toString());
        Team activatedTeam = teamService.activateTeam(teamRequest);
        log.debug("Received response from Teams service: '{}'", activatedTeam.toString());

        log.info("Team activated: '{}'", activatedTeam.getId());
        return new RichMessage(String.format("Thanks, new Team for '%s' activated", text));
    }
}
