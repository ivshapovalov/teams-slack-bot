package ua.com.juja.microservices.teams.slackbot.controller;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.com.juja.microservices.teams.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.service.TeamSlackbotService;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1/commands")
@Slf4j
public class TeamSlackbotController {

    private final SlackNameHandlerService slackNameHandlerService;
    @Value("${slack.slashCommandToken}")
    private String slackToken;
    private TeamSlackbotService teamSlackbotService;

    @Inject
    public TeamSlackbotController(TeamSlackbotService teamSlackbotService,
                                  SlackNameHandlerService slackNameHandlerService) {
        this.teamSlackbotService = teamSlackbotService;
        this.slackNameHandlerService = slackNameHandlerService;
    }

    @PostMapping(value = "/teams/activate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommandActivateTeam(@RequestParam("token") String token,
                                                         @RequestParam("user_name") String fromUser,
                                                         @RequestParam("text") String text) {
        log.debug("Received slash command 'Activate team' from user: '{}' text: '{}' token: '{}'",
                fromUser, text, token);

        if (!token.equals(slackToken)) {
            log.warn("Received invalid slack token: '{}' in command 'Activate team' from user: '{}'. Returns to " +
                            "slack!",
                    token, fromUser);
            return getRichMessageInvalidSlackCommand();
        }
        String response = "ERROR. Something wrong and new team didn't activated.";

        log.debug("Started create slackParsedCommand from user '{}' and text '{}'", fromUser, text);
        SlackParsedCommand slackParsedCommand = slackNameHandlerService.createSlackParsedCommand(fromUser, text);
        log.debug("Finished create slackParsedCommand");

        log.debug("Started create TeamRequest");
        TeamRequest teamRequest = new TeamRequest(slackParsedCommand);
        log.debug("Finished create TeamRequest");

        log.debug("Send activate team request to Teams service. Team: '{}'", teamRequest.toString());
        Team activatedTeam = teamSlackbotService.activateTeam(teamRequest);
        log.debug("Received response from Teams service: '{}'", activatedTeam.toString());

        log.debug("Started getTeamMemberSlackNames for team '{}' ", activatedTeam.toString());
        Set<String> slackNames = slackNameHandlerService.getTeamMemberSlackNames(activatedTeam);
        log.debug("Finished getTeamMemberSlackNames for team '{}'. slacknames is '{}' ", activatedTeam.toString()
                , slackNames);

        response = String.format("Thanks, new Team for users '%s' activated",
                slackNames.stream().collect(Collectors.joining(",")));

        log.info("'Activate team' command processed : user: '{}' text: '{}' and sent response into slack: '{}'",
                fromUser, text, response);

        return new RichMessage(response);
    }

    @PostMapping(value = "/teams/deactivate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommandDeactivateTeam(@RequestParam("token") String token,
                                                           @RequestParam("user_name") String fromUser,
                                                           @RequestParam("text") String text) {
        //TODO Should be implemented feature SLB-F2
        return null;
    }

    @PostMapping(value = "/team", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommandGetTeam(@RequestParam("token") String token,
                                                    @RequestParam("user_name") String fromUser,
                                                    @RequestParam("text") String text) {
        //TODO Should be implemented feature SLB-F3
        return null;
    }

    @PostMapping(value = "/myteam", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommandGetMyTeam(@RequestParam("token") String token,
                                                      @RequestParam("user_name") String fromUser,
                                                      @RequestParam("text") String text) {
        //TODO Should be implemented feature SLB-F4
        return null;
    }

    private RichMessage getRichMessageInvalidSlackCommand() {
        return new RichMessage("Sorry! You're not lucky enough to use our slack command.");
    }

}
