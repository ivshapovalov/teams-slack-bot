package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
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
import java.util.ArrayList;
import java.util.Collections;
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
        Utils.checkNull(text, "Text must not be null!");

        List<String> slackNames = SlackNameHandler.getSlackNamesFromText(text);

        Set<User> users = new HashSet<>(userService.findUsersBySlackNames(slackNames));
        Set<String> uuids = users.stream()
                .map(User::getUuid)
                .collect(Collectors.toSet());

        if (uuids.size() != TEAM_SIZE) {
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command." +
                    " But size of the team must be %s.", uuids.size(), TEAM_SIZE));
        }
        TeamRequest teamRequest = new TeamRequest(uuids);
        Team activatedTeam = teamRepository.activateTeam(teamRequest);
        checkTeamMembersEquality(teamRequest.getMembers(), activatedTeam.getMembers());
        log.info("Team activated: '{}'", activatedTeam.getId());
        return activatedTeam;
    }

    private void checkTeamMembersEquality(Set<String> requestMembers, Set<String> responseMembers) {
        log.debug("Before check members equality. Request '{}'. Response '{}'", requestMembers, responseMembers);
        if (!(requestMembers.containsAll(responseMembers) && responseMembers.containsAll(requestMembers))) {
            Exception ex = new Exception("Team members is not equals in request and response from Teams Service");
            ApiError apiError = new ApiError(
                    500, "BotInternalError",
                    ex.getMessage(),
                    ex.getMessage(),
                    ex.getMessage(),
                    Collections.singletonList("")
            );
            throw new TeamExchangeException(apiError, ex);
        }
    }

    @Override
    public Set<String> getTeam(String text) {
        Utils.checkNull(text, "Text must not be null!");
        List<String> slackNames = SlackNameHandler.getSlackNamesFromText(text);
        if (slackNames.size() != 1) {
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command." +
                    " But expect one slack name.", slackNames.size()));
        }
        List<User> users = userService.findUsersBySlackNames(slackNames);
        if (users.size() != slackNames.size()) {
            throwUserExchangeException();
        }
        String uuid = users.get(0).getUuid();
        Team team = teamRepository.getTeam(uuid);
        List<User> teamUsers = userService.findUsersByUuids(new ArrayList<>(team.getMembers()));
        Set<String> teamSlackNames = teamUsers.stream()
                .map(User::getSlack)
                .collect(Collectors.toSet());

        log.info("Team got: '{}'", team.getId());
        return teamSlackNames;
    }

    private void throwUserExchangeException() {
        Exception ex = new Exception("Users count is not equals in request and response from Users Service");
        ApiError apiError = new ApiError(
                500, "BotInternalError",
                ex.getMessage(),
                ex.getMessage(),
                ex.getMessage(),
                Collections.singletonList("")
        );
        throw new UserExchangeException(apiError, ex);
    }

    @Override
    public Set<String> deactivateTeam(String text) {
        List<String> slackNames = SlackNameHandler.getSlackNamesFromText(text);
        if (slackNames.size() != 1) {
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command." +
                    " But expect one slack name.", slackNames.size()));
        }
        List<User> users = userService.findUsersBySlackNames(slackNames);
        if (users.size() != slackNames.size()) {
            throwUserExchangeException();
        }
        String uuid = users.get(0).getUuid();
        Team deactivatedTeam = teamRepository.deactivateTeam(uuid);
        List<User> teamUsers = userService.findUsersByUuids(new ArrayList<>(deactivatedTeam.getMembers()));
        Set<String> teamSlackNames = teamUsers.stream()
                .map(User::getSlack)
                .collect(Collectors.toSet());
        log.info("Team deactivated: '{}'", deactivatedTeam.getId());
        return teamSlackNames;
    }
}
