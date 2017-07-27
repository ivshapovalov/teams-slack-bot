package ua.com.juja.microservices.teams.slackbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.BaseBotException;
import ua.com.juja.microservices.teams.slackbot.exceptions.ExceptionsHandler;
import ua.com.juja.microservices.teams.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.service.TeamSlackbotService;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;
import ua.com.juja.microservices.teams.slackbot.util.Util;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

@RestController
@RequestMapping(value = "/v1/commands")
@Slf4j
public class TeamSlackbotController {

    private final SlackNameHandlerService slackNameHandlerService;
    @Value("${slack.slashCommandToken}")
    private String slackToken;
    private TeamSlackbotService teamSlackbotService;

    private ExceptionsHandler exceptionsHandler;

    @Inject
    public TeamSlackbotController(TeamSlackbotService teamSlackbotService,
                                  SlackNameHandlerService slackNameHandlerService,
                                  ExceptionsHandler exceptionsHandler) {
        this.teamSlackbotService = teamSlackbotService;
        this.slackNameHandlerService = slackNameHandlerService;
        this.exceptionsHandler = exceptionsHandler;

    }

    @PostMapping(value = "/teams/activate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void onReceiveSlashCommandActivateTeam(@RequestParam("token") String token,
                                                         @RequestParam("user_name") String fromUser,
                                                         @RequestParam("text") String text,
                                                         @RequestParam("response_url") String responseUrl,
                                                         HttpServletResponse response) throws IOException {
        log.debug("Received slash command 'Activate team' from user: '{}' text: '{}' token: '{}' response_url: '{}'",
                fromUser, text, token, responseUrl);

        exceptionsHandler.setResponseUrl(responseUrl);

        if (!token.equals(slackToken)) {
            log.warn("Received invalid slack token: '{}' in command 'Activate team' from user: '{}'. Returns to " +
                            "slack!",
                    token, fromUser);
            sendFirstMessage(response, getMessageInvalidSlackCommand());
        }

        String message = String.format("Thanks, Activate Team job started!");
        log.info("'Activate team' job started : user: '{}' text: '{}' and sent response into slack: '{}'",
                fromUser, text, message);
        //sendFirstMessage(response, message);

        log.debug("Started create slackParsedCommand from user '{}' and text '{}'", fromUser, text);
        SlackParsedCommand slackParsedCommand = slackNameHandlerService.createSlackParsedCommand(fromUser, text);
        log.debug("Finished create slackParsedCommand");

        log.debug("Started create TeamRequest");
        TeamRequest teamRequest = new TeamRequest(slackParsedCommand,responseUrl);
        log.debug("Finished create TeamRequest");

        log.debug("Send activate team request to Teams service. Team: '{}'", teamRequest.toString());
        Team activatedTeam = teamSlackbotService.activateTeam(teamRequest);
        log.debug("Received response from Teams service: '{}'", activatedTeam.toString());

        log.debug("Started createUsersSetFromUuids for team '{}' ", activatedTeam.toString());
        Set<String> slackNames = slackNameHandlerService.createUsersSetFromUuids(activatedTeam.getMembers());
        log.debug("Finished createUsersSetFromUuids for team '{}'. slacknames is '{}' ", activatedTeam.toString()
                , slackNames);

        message = String.format("Thanks, new Team for users '%s' activated",
                slackNames.stream().collect(Collectors.joining(",")));

        log.info("'Activate team' command processed : user: '{}' text: '{}' and sent response into slack: '{}'",
                fromUser, text, response);
        RichMessage richMessage = new RichMessage(message);
        //Util.sendResultRichMessage(responseUrl, richMessage);
        //return new RichMessage(response);
    }

    private void sendFirstMessage(HttpServletResponse response,String message) throws IOException {
        PrintWriter wr = response.getWriter();
        response.setStatus(200);
        wr.print(message);
        wr.flush();
        wr.close();
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

    private String getMessageInvalidSlackCommand() {
        return "Sorry! You're not lucky enough to use our slack command.";
    }

}
