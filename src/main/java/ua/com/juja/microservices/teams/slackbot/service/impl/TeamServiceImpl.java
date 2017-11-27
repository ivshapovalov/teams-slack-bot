package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.TeamExchangeException;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;
import ua.com.juja.microservices.teams.slackbot.model.teams.ActivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.DeactivateTeamRequest;
import ua.com.juja.microservices.teams.slackbot.model.teams.Team;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.repository.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;
import ua.com.juja.microservices.teams.slackbot.service.UserService;
import ua.com.juja.microservices.teams.slackbot.util.SlackIdHandler;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public Team activateTeam(String fromUserId, String text) {
        Utils.checkNull(text, "Text must not be null!");
        Utils.checkNull(fromUserId, "FromUserId must not be null!");
        Set<String> slackIds = SlackIdHandler.getSlackIdsFromText(text);
        if (slackIds.size() != TEAM_SIZE) {
            throw new WrongCommandFormatException(String.format("We found %d slack id in your command." +
                    " But size of the team must be %s.", slackIds.size(), TEAM_SIZE));
        }
        slackIds.add(fromUserId);
        Set<User> users = new HashSet<>(userService.findUsersBySlackIds(new ArrayList<>(slackIds)));
        Map<String, String> usersMap = users.stream()
                .collect(Collectors.toMap(User::getSlackId, User::getUuid, (e1, e2) -> e1,
                        LinkedHashMap::new));
        String fromUserUuid = usersMap.get(fromUserId);
        ActivateTeamRequest activateTeamRequest = new ActivateTeamRequest(fromUserUuid, new HashSet<>
                (extractMembersFromMap(fromUserId, usersMap, TEAM_SIZE)));
        Team activatedTeam = teamRepository.activateTeam(activateTeamRequest);
        checkTeamMembersEquality(activateTeamRequest.getMembers(), activatedTeam.getMembers());
        log.info("Team activated: '{}'", activatedTeam.getId());
        return activatedTeam;
    }

    private List<String> extractMembersFromMap(String fromUserId, Map<String, String> usersMap, int expectedSize) {
        log.debug("Before extract members from users map: '{}'. Expected size '{}'. FromUserId is '{}'", usersMap,
                expectedSize, fromUserId);
        List<String> uuids;
        if (usersMap.size() == expectedSize) {
            uuids = new ArrayList<>(usersMap.values());
        } else {
            uuids = usersMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(fromUserId))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
        }
        log.debug("After extract members from users map. Uuids is '{}'", uuids);
        return uuids;
    }

    private void checkTeamMembersEquality(Set<String> requestMembers, Set<String> responseMembers) {
        log.debug("Before check team members equality. Request to Team service '{}'. Response from Team service '{}'",
                requestMembers, responseMembers);
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
        Set<String> slackIds = SlackIdHandler.getSlackIdsFromText(text);
        if (slackIds.size() != 1) {
            throw new WrongCommandFormatException(String.format("We found %d slack id in your command." +
                    " But expect one slack id.", slackIds.size()));
        }
        List<User> users = userService.findUsersBySlackIds(new ArrayList<>(slackIds));
        String uuid = users.get(0).getUuid();
        Team team = teamRepository.getTeam(uuid);
        List<User> teamUsers = userService.findUsersByUuids(new ArrayList<>(team.getMembers()));
        Set<String> teamSlackIds = teamUsers.stream()
                .map(User::getSlackId)
                .collect(Collectors.toSet());
        log.info("Team got: '{}'", team.getId());
        return teamSlackIds;
    }

    @Override
    public Set<String> deactivateTeam(String fromUser, String text) {
        Set<String> slackIds = SlackIdHandler.getSlackIdsFromText(text);
        if (slackIds.size() != 1) {
            throw new WrongCommandFormatException(String.format("We found %d slack id in your command." +
                    " But expect one slack id.", slackIds.size()));
        }
        slackIds.add(fromUser);
        List<User> users = userService.findUsersBySlackIds(new ArrayList<>(slackIds));
        Map<String, String> usersMap = users.stream()
                .collect(Collectors.toMap(User::getSlackId, User::getUuid, (e1, e2) -> e1,
                        LinkedHashMap::new));
        String fromUserUuid = usersMap.get(fromUser);
        String uuid = extractMembersFromMap(fromUser, usersMap, 1).get(0);
        DeactivateTeamRequest deactivateTeamRequest = new DeactivateTeamRequest(fromUserUuid, uuid);
        Team deactivatedTeam = teamRepository.deactivateTeam(deactivateTeamRequest);
        List<User> teamUsers = userService.findUsersByUuids(new ArrayList<>(deactivatedTeam.getMembers()));
        Set<String> teamSlackIds = teamUsers.stream()
                .map(User::getSlackId)
                .collect(Collectors.toSet());
        log.info("Team deactivated: '{}'", deactivatedTeam.getId());
        return teamSlackIds;
    }
}
