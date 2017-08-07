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
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.service.TeamService;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
@RequestMapping(value = "/v1/commands")
@Slf4j
public class TeamSlackbotController {

    private final static String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command";
    private final static String ACTIVATE_TEAM_MESSAGE = "Thanks, Activate Team job started!";
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

    @PostMapping(value = "/teams/activate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void onReceiveSlashCommandActivateTeam(@RequestParam("token") String token,
                                                  @RequestParam("user_name") String fromUser,
                                                  @RequestParam("text") String text,
                                                  @RequestParam("response_url") String responseUrl,
                                                  HttpServletResponse response) throws IOException {
        log.debug("Received slash command 'Activate team' from user: '{}' text: '{}' token: '{}' response_url: '{}'",
                fromUser, text, token, responseUrl);
        exceptionsHandler.setResponseUrl(responseUrl);
        if (isTokenCorrect(token, fromUser, response, "Activate team")) {

            sendInstantResponseMessage(response, ACTIVATE_TEAM_MESSAGE);

            log.debug("Started activate team request to Team service. Text: '{}'", text);
            Team activatedTeam = teamService.activateTeam(text);
            log.debug("Finished activate team in Team service. New Team: '{}'", activatedTeam.toString());

            RichMessage message = new RichMessage(String.format("Thanks, new Team for '%s' activated", text));
            sendDelayedResponseMessage(responseUrl, message);

            log.info("'Activate team' command processed : user: '{}' text: '{}' and sent message to slack: '{}'",
                    fromUser, text, message.getText());
        }
    }

    private void sendInstantResponseMessage(HttpServletResponse response, String message) throws IOException {
        log.debug("Started send first response message to slack '{}' ", ACTIVATE_TEAM_MESSAGE);
        PrintWriter printWriter = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        printWriter.print(message);
        printWriter.flush();
        printWriter.close();
        log.debug("Finished send first response message to slack '{}' ", ACTIVATE_TEAM_MESSAGE);
        log.info("Sent first response message to slack '{}' ", message);
    }

    private void sendDelayedResponseMessage(String responseUrl, RichMessage message) {
        log.debug("Started send delayed response message to slack '{}' ", message.getText());
        restTemplate.postForObject(responseUrl, message, String.class);
        log.debug("Finished send delayed response message to slack '{}' ", message.getText());
    }

    private boolean isTokenCorrect(String token, String fromUser, HttpServletResponse response, String commandName)
            throws
            IOException {
        if (!token.equals(slackToken)) {
            log.warn("Received invalid slack token: '{}' in command '{}' from user: '{}'. Returns to " +
                    "slack!", token, commandName, fromUser);
            sendInstantResponseMessage(response, SORRY_MESSAGE);
            return false;
        }
        return true;
    }

    @PostMapping(value = "/teams/deactivate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void onReceiveSlashCommandDeactivateTeam(@RequestParam("token") String token,
                                                    @RequestParam("user_name") String fromUser,
                                                    @RequestParam("text") String text,
                                                    @RequestParam("response_url") String responseUrl,
                                                    HttpServletResponse response) {
        //TODO Should be implemented feature SLB-F2
    }

    @PostMapping(value = "/team", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void onReceiveSlashCommandGetTeam(@RequestParam("token") String token,
                                             @RequestParam("user_name") String fromUser,
                                             @RequestParam("text") String text,
                                             @RequestParam("response_url") String responseUrl,
                                             HttpServletResponse response) {
        //TODO Should be implemented feature SLB-F3
    }

    @PostMapping(value = "/myteam", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void onReceiveSlashCommandGetMyTeam(@RequestParam("token") String token,
                                               @RequestParam("user_name") String fromUser,
                                               @RequestParam("text") String text,
                                               @RequestParam("response_url") String responseUrl,
                                               HttpServletResponse response) {
        //TODO Should be implemented feature SLB-F4
    }

}
