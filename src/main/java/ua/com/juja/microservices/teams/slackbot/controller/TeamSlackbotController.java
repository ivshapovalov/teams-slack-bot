package ua.com.juja.microservices.teams.slackbot.controller;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.ExceptionsHandler;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/" + "${teams.slackbot.rest.api.version}" + "${teams.slackbot.commandsUrl}")
@Slf4j
public class TeamSlackbotController {

    private final static String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command";
    private final static String ACTIVATE_TEAM_MESSAGE = "Thanks, Activate Team job started!";
    private final static String GET_TEAM_MESSAGE = "Thanks, Get Team for user '%s' job started!";
    private final static String GET_MY_TEAM_MESSAGE = "Thanks, Get My Team for user '%s' job started!";
    private final static String DEACTIVATE_TEAM_MESSAGE = "Thanks, Deactivate Team for user '%s' job started!";

    private final RestTemplate restTemplate;
    private final TeamService teamService;
    private final ExceptionsHandler exceptionsHandler;
    @Value("${slack.slashCommandToken}")
    private String slackToken;

    @Inject
    public TeamSlackbotController(TeamService teamService,
                                  ExceptionsHandler exceptionsHandler,
                                  RestTemplate restTemplate) {
        this.teamService = teamService;
        this.exceptionsHandler = exceptionsHandler;
        this.restTemplate = restTemplate;
    }

    @PostMapping(value = "${teams.slackbot.endpoint.activateTeam}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void onReceiveSlashCommandActivateTeam(@RequestParam("token") String token,
                                                  @RequestParam("user_name") String fromUser,
                                                  @RequestParam("text") String text,
                                                  @RequestParam("response_url") String responseUrl,
                                                  HttpServletResponse response) throws IOException {
        exceptionsHandler.setResponseUrl(responseUrl);
        if (isTokenCorrect(token, response)) {
            sendInstantResponseMessage(response, ACTIVATE_TEAM_MESSAGE);
            teamService.activateTeam(text);
            RichMessage message = new RichMessage(String.format("Thanks, new Team for '%s' activated", text));
            sendDelayedResponseMessage(responseUrl, message);
            log.info("'Activate team' command processed : user: '{}' text: '{}' and sent message to slack: '{}'",
                    fromUser, text, message.getText());
        }
    }

    @PostMapping(value = "${teams.slackbot.endpoint.deactivateTeam}",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void onReceiveSlashCommandDeactivateTeam(@RequestParam("token") String token,
                                                    @RequestParam("user_name") String fromUser,
                                                    @RequestParam("text") String text,
                                                    @RequestParam("response_url") String responseUrl,
                                                    HttpServletResponse response) throws IOException {
        exceptionsHandler.setResponseUrl(responseUrl);
        if (isTokenCorrect(token, response)) {
            sendInstantResponseMessage(response, String.format(DEACTIVATE_TEAM_MESSAGE, text));
            Set<String> slackNames = teamService.deactivateTeam(text);
            RichMessage message = new RichMessage(String.format("Thanks, Team '%s' deactivated",
                    slackNames.stream().collect(Collectors.joining(" "))));
            restTemplate.postForObject(responseUrl, message, String.class);
            log.info("'Deactivate team' command processed : user: '{}' text: '{}' and sent message to slack: '{}'",
                    fromUser, text, message.getText());
        }
    }

    @PostMapping(value = "${teams.slackbot.endpoint.getTeam}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void onReceiveSlashCommandGetTeam(@RequestParam("token") String token,
                                             @RequestParam("user_name") String fromUser,
                                             @RequestParam("text") String text,
                                             @RequestParam("response_url") String responseUrl,
                                             HttpServletResponse response) throws IOException {
        exceptionsHandler.setResponseUrl(responseUrl);
        if (isTokenCorrect(token, response)) {
            sendInstantResponseMessage(response, String.format(GET_TEAM_MESSAGE, text));
            Set<String> slackNames = teamService.getTeam(text);
            RichMessage message = new RichMessage(String.format("Thanks, Team for '%s' is '%s'",
                    text, slackNames.stream().collect(Collectors.joining(" "))));
            sendDelayedResponseMessage(responseUrl, message);
            log.info("'Get team' command processed : user: '{}' text: '{}' and sent message to slack: '{}'",
                    fromUser, text, message.getText());
        }
    }

    @PostMapping(value = "${teams.slackbot.endpoint.getMyTeam}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void onReceiveSlashCommandGetMyTeam(@RequestParam("token") String token,
                                               @RequestParam("user_name") String fromUser,
                                               @RequestParam("response_url") String responseUrl,
                                               HttpServletResponse response) throws IOException {
        exceptionsHandler.setResponseUrl(responseUrl);
        if (isTokenCorrect(token, response)) {
            fromUser = fromUser.startsWith("@") ? fromUser : "@" + fromUser;
            sendInstantResponseMessage(response, String.format(GET_MY_TEAM_MESSAGE, fromUser));
            Set<String> slackNames = teamService.getTeam(fromUser);
            RichMessage message = new RichMessage(String.format("Thanks, Team for user '%s' is '%s'",
                    fromUser, slackNames.stream().collect(Collectors.joining(" "))));
            sendDelayedResponseMessage(responseUrl, message);
            log.info("'Get my team' command processed : user: '{}' and sent message to slack: '{}'",
                    fromUser, message.getText());
        }
    }

    private void sendInstantResponseMessage(HttpServletResponse response, String message) throws IOException {
        log.debug("Before sending instant response message '{}' ", message);
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter printWriter = response.getWriter();
        printWriter.print(message);
        printWriter.flush();
        printWriter.close();
        log.info("Sent instant response message to slack '{}' ", message);
    }

    private void sendDelayedResponseMessage(String responseUrl, RichMessage message) {
        log.debug("Before sending delayed response message '{}' to slack url '{}' ", message, responseUrl);
        String response = restTemplate.postForObject(responseUrl, message, String.class);
        log.debug("After sending delayed response message. Response is '{}'", response);
    }

    private boolean isTokenCorrect(String token, HttpServletResponse response)
            throws IOException {
        if (!token.equals(slackToken)) {
            sendInstantResponseMessage(response, SORRY_MESSAGE);
            return false;
        }
        return true;
    }
}
