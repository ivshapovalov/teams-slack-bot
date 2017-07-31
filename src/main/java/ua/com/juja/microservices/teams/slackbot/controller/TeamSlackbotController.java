package ua.com.juja.microservices.teams.slackbot.controller;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.com.juja.microservices.teams.slackbot.exceptions.ExceptionsHandler;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.service.TeamSlackbotService;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1/commands")
@Slf4j
public class TeamSlackbotController {

    private final SlackNameHandlerService slackNameHandlerService;
    private final String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command";
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

        checkToken(token, fromUser, response);

        String message = String.format("Thanks, Activate Team job started!");
        log.debug("Started send first response message to slack '{}' ", message);
        sendResponseMessage(response, message);
        log.debug("Finished send first response message to slack '{}' ", message);

        log.debug("Started extract members from text '{}'", text);
        Set<String> members = slackNameHandlerService.getUuidsFromText(text);
        log.debug("Finished extract members '{}' from text '{}'", members,text);

        log.debug("Started create TeamRequest");
        TeamRequest teamRequest = new TeamRequest(members);
        log.debug("Finished create TeamRequest");

        log.debug("Send activate team request to Teams service. Team: '{}'", teamRequest.toString());
        Team activatedTeam = teamSlackbotService.activateTeam(teamRequest);
        log.debug("Received response from Teams service: '{}'", activatedTeam.toString());

        log.debug("Started getSlackNamesFromUuids for team '{}' ", activatedTeam.toString());
        Set<String> slackNames = slackNameHandlerService.getSlackNamesFromUuids(activatedTeam.getMembers());
        log.debug("Finished getSlackNamesFromUuids for team '{}'. slacknames is '{}' ", activatedTeam.toString()
                , slackNames);

        message = String.format("Thanks, new Team for members '%s' activated",
                slackNames.stream().collect(Collectors.joining(",")));
        log.info("'Activate team' command processed : user: '{}' text: '{}' and sent message into slack: '{}'",
                fromUser, text, message);
        RichMessage richMessage = new RichMessage(message);
        Utils.sendPostResponseAsRichMessage(responseUrl, richMessage);
    }

    private void checkToken(@RequestParam("token") String token, @RequestParam("user_name") String fromUser, HttpServletResponse response) throws IOException {
        if (!token.equals(slackToken)) {
            log.warn("Received invalid slack token: '{}' in command 'Activate team' from user: '{}'. Returns to " +
                            "slack!",
                    token, fromUser);
            sendResponseMessage(response, SORRY_MESSAGE);
        }
    }

    private void sendResponseMessage(HttpServletResponse response, String message) throws IOException {
        PrintWriter wr = response.getWriter();
        response.setStatus(200);
        wr.print(message);
        wr.flush();
        wr.close();
        log.info("Sent first response message to slack '{}' ", message);
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

}
