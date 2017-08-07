package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.repository.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;
import ua.com.juja.microservices.teams.slackbot.service.UserService;
import ua.com.juja.microservices.teams.slackbot.util.SlackNameHandler;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ivan Shapovalov
 */
@Slf4j
@Service
public class TeamServiceImpl implements TeamService {

    private static final int TEAM_SIZE = 4;

    private final UserService userService;

    private final TeamRepository teamRepository;

    @Inject
    public TeamServiceImpl(TeamRepository teamRepository, UserService userService) {
        this.teamRepository = teamRepository;
        this.userService = userService;
    }

    @Override
    public Team activateTeam(String text) {
        log.debug("Started extract members from text '{}'", text);
        Utils.checkNull(text, "Text must not be null!");

        List<String> slackNames = SlackNameHandler.getSlackNamesFromText(text);

        Set<User> users = new HashSet<>(userService.findUsersBySlackNames(slackNames));
        Set<String> uuids = users.stream()
                .map(User::getUuid)
                .collect(Collectors.toSet());

        if (uuids.size() != TEAM_SIZE) {
            log.warn("Members size is not equals '{}'" + TEAM_SIZE);
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command." +
                    " But size of the team must be %s.", uuids.size(), TEAM_SIZE));
        }
        log.debug("Started create TeamRequest");
        TeamRequest teamRequest = new TeamRequest(uuids);
        log.debug("Finished create TeamRequest");

        log.debug("Send activate team request to Teams repository. Team: '{}'", teamRequest.toString());
        Team activatedTeam = teamRepository.activateTeam(teamRequest);
        log.debug("Received response from Teams repository: '{}'", activatedTeam.toString());

        log.info("Team activated: '{}'", activatedTeam.getId());
        return activatedTeam;
    }
}
