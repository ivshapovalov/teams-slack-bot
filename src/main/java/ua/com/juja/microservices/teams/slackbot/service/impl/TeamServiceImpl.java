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
    public Set<String> activateTeam(String fromUserId, String text) {
        Utils.checkNull(text, "Text must not be null!");
        Utils.checkNull(fromUserId, "FromUserId must not be null!");
        Set<String> slackIds = SlackIdHandler.getSlackIdsFromText(text);
        if (slackIds.size() != TEAM_SIZE) {
            throw new WrongCommandFormatException(String.format("We found %d slack id in your command." +
                    " But size of the team must be %s.", slackIds.size(), TEAM_SIZE));
        }
        slackIds.add(fromUserId);
        Set<User> users = new HashSet<>(userService.findUsersBySlackIds(new ArrayList<>(slackIds)));
        String fromUserUuid = getFromUserUuid(fromUserId, users);
        Set<String> membersUuids = getMembersUuids(fromUserId, users, TEAM_SIZE);
        ActivateTeamRequest activateTeamRequest = new ActivateTeamRequest(fromUserUuid, membersUuids);
        Team activatedTeam = teamRepository.activateTeam(activateTeamRequest);
        checkTeamMembersEquality(activateTeamRequest.getMembers(), activatedTeam.getMembers());
        List<User> teamUsers = userService.findUsersByUuids(new ArrayList<>(activatedTeam.getMembers()));
        Set<String> teamSlackIds = teamUsers.stream()
                .map(User::getSlackId)
                .collect(Collectors.toSet());
        log.info("Team activated: '{}'", activatedTeam.getId());
        return teamSlackIds;
    }

    private String getFromUserUuid(String fromUserId, Set<User> users) {
        log.debug("Before extract fromUserId from users : '{}'.FromUserId is '{}'", users, fromUserId);
        User fromUser = users.stream()
                .filter(user -> user.getSlackId().equals(fromUserId))
                .collect(Collectors.toList()).get(0);
        log.debug("After extract fromUserId from users map. Uuid is '{}'", fromUser.getUuid());
        return fromUser.getUuid();
    }

    private Set<String> getMembersUuids(String fromUserId, Set<User> users, int expectedSize) {
        //Set<users> contain all users of request (fromUserId and all users in text)
        //Response depends on:
        //case Activate Team    -  is fromUser a member of new Team or not
        //case Deactivate Team  -  is fromUser deactivate his team or not
        log.debug("Before extract members from users : '{}'. Expected size '{}'. FromUserId is '{}'", users,
                expectedSize, fromUserId);
        Set<String> uuids;
        if (users.size() == expectedSize) {
            //if fromUser in new Team or fromUser deactivate his Team
            uuids = users.stream().map(User::getUuid).collect(Collectors.toSet());
        } else {
            //if fromUser not in new Team or fromUser deactivate not his Team
            //That's why we exclude him from response
            uuids = users.stream()
                    .filter(user -> !user.getSlackId().equals(fromUserId))
                    .map(User::getUuid)
                    .collect(Collectors.toSet());
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
    public Set<String> deactivateTeam(String fromUserId, String text) {
        Set<String> slackIds = SlackIdHandler.getSlackIdsFromText(text);
        if (slackIds.size() != 1) {
            throw new WrongCommandFormatException(String.format("We found %d slack id in your command." +
                    " But expect one slack id.", slackIds.size()));
        }
        slackIds.add(fromUserId);
        Set<User> users = new HashSet<>(userService.findUsersBySlackIds(new ArrayList<>(slackIds)));
        String fromUserUuid = getFromUserUuid(fromUserId, users);
        String uuid = new ArrayList<>(getMembersUuids(fromUserId, users, 1)).get(0);
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
