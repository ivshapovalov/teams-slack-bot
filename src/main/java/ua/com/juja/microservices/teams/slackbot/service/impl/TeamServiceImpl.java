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
import ua.com.juja.microservices.teams.slackbot.util.SlackUserHandler;
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
    public Set<String> activateTeam(String fromSlackUser, String text) {
        Utils.checkNull(text, "Text must not be null!");
        Utils.checkNull(fromSlackUser, "FromSlackUser must not be null!");
        Set<String> slackUsers = SlackUserHandler.getSlackUsersFromText(text);
        if (slackUsers.size() != TEAM_SIZE) {
            throw new WrongCommandFormatException(String.format("We found %d slack user in your command." +
                    " But size of the team must be %s.", slackUsers.size(), TEAM_SIZE));
        }
        slackUsers.add(fromSlackUser);
        Set<User> users = new HashSet<>(userService.findUsersBySlackUsers(new ArrayList<>(slackUsers)));
        String fromUserUuid = getFromUserUuid(fromSlackUser, users);
        Set<String> membersUuids = getMembersUuids(fromSlackUser, users, TEAM_SIZE);
        ActivateTeamRequest activateTeamRequest = new ActivateTeamRequest(fromUserUuid, membersUuids);
        Team activatedTeam = teamRepository.activateTeam(activateTeamRequest);
        checkTeamMembersEquality(activateTeamRequest.getMembers(), activatedTeam.getMembers());
        List<User> teamUsers = userService.findUsersByUuids(new ArrayList<>(activatedTeam.getMembers()));
        Set<String> teamSlackUsers = teamUsers.stream()
                .map(User::getSlackUser)
                .collect(Collectors.toSet());
        log.info("Team activated: '{}'", activatedTeam.getId());
        return teamSlackUsers;
    }

    private String getFromUserUuid(String fromSlackUser, Set<User> users) {
        log.debug("Before extract fromSlackUser from users : '{}'. fromSlackUser is '{}'", users, fromSlackUser);
        User fromUser = users.stream()
                .filter(user -> user.getSlackUser().equals(fromSlackUser))
                .collect(Collectors.toList()).get(0);
        log.debug("After extract fromSlackUser from users map. Uuid is '{}'", fromUser.getUuid());
        return fromUser.getUuid();
    }

    private Set<String> getMembersUuids(String fromSlackUser, Set<User> users, int expectedSize) {
        //Set<users> contain all users of request (fromSlackUser and all users in text)
        //Response depends on:
        //case Activate Team    -  is fromSlackUser a member of new Team or not
        //case Deactivate Team  -  is fromSlackUser deactivate his team or not
        log.debug("Before extract members from users : '{}'. Expected size '{}'. fromSlackUser is '{}'", users,
                expectedSize, fromSlackUser);
        Set<String> uuids;
        if (users.size() == expectedSize) {
            //if fromUser in new Team or fromUser deactivate his Team
            uuids = users.stream().map(User::getUuid).collect(Collectors.toSet());
        } else {
            //if fromUser not in new Team or fromUser deactivate not his Team
            //That's why we exclude him from response
            uuids = users.stream()
                    .filter(user -> !user.getSlackUser().equals(fromSlackUser))
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
        Set<String> slackUsers = SlackUserHandler.getSlackUsersFromText(text);
        if (slackUsers.size() != 1) {
            throw new WrongCommandFormatException(String.format("We found %d slack user in your command." +
                    " But expect one slack user.", slackUsers.size()));
        }
        List<User> users = userService.findUsersBySlackUsers(new ArrayList<>(slackUsers));
        String uuid = users.get(0).getUuid();
        Team team = teamRepository.getTeam(uuid);
        List<User> teamUsers = userService.findUsersByUuids(new ArrayList<>(team.getMembers()));
        Set<String> teamSlackUsers = teamUsers.stream()
                .map(User::getSlackUser)
                .collect(Collectors.toSet());
        log.info("Team got: '{}'", team.getId());
        return teamSlackUsers;
    }

    @Override
    public Set<String> deactivateTeam(String fromSlackUser, String text) {
        Set<String> slackUsers = SlackUserHandler.getSlackUsersFromText(text);
        if (slackUsers.size() != 1) {
            throw new WrongCommandFormatException(String.format("We found %d slack user in your command." +
                    " But expect one slack user.", slackUsers.size()));
        }
        slackUsers.add(fromSlackUser);
        Set<User> users = new HashSet<>(userService.findUsersBySlackUsers(new ArrayList<>(slackUsers)));
        String fromUserUuid = getFromUserUuid(fromSlackUser, users);
        String uuid = new ArrayList<>(getMembersUuids(fromSlackUser, users, 1)).get(0);
        DeactivateTeamRequest deactivateTeamRequest = new DeactivateTeamRequest(fromUserUuid, uuid);
        Team deactivatedTeam = teamRepository.deactivateTeam(deactivateTeamRequest);
        List<User> teamUsers = userService.findUsersByUuids(new ArrayList<>(deactivatedTeam.getMembers()));
        Set<String> teamSlackUsers = teamUsers.stream()
                .map(User::getSlackUser)
                .collect(Collectors.toSet());
        log.info("Team deactivated: '{}'", deactivatedTeam.getId());
        return teamSlackUsers;
    }
}
