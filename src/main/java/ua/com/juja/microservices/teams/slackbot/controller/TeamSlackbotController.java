package ua.com.juja.microservices.teams.slackbot.controller;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.com.juja.microservices.teams.slackbot.service.TeamSlackbotService;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

@RestController
@RequestMapping(value = "/v1/commands")
@Slf4j
public class TeamSlackbotController {

    private final SlackNameHandlerService slackNameHandlerService;
    @Value("${slack.slashCommandToken}")
    private String slackToken;
    private TeamSlackbotService teamSlackbotService;

    private BackgroundJobController backgroundJobController;
    private ExecutorService backgroundExecutor = newSingleThreadExecutor();

    @Inject
    public TeamSlackbotController(TeamSlackbotService teamSlackbotService,
                                  SlackNameHandlerService slackNameHandlerService,
                                  BackgroundJobController backgroundJobController) {
        this.teamSlackbotService = teamSlackbotService;
        this.slackNameHandlerService = slackNameHandlerService;
        this.backgroundJobController = backgroundJobController;
    }

    @PostMapping(value = "/teams/activate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommandActivateTeam(@RequestParam("token") String token,
                                                         @RequestParam("user_name") String fromUser,
                                                         @RequestParam("text") String text,
                                                         @RequestParam("response_url") String responseUrl) {
        log.debug("Received slash command 'Activate team' from user: '{}' text: '{}' token: '{}' response_url: '{}'",
                fromUser, text, token, responseUrl);

        if (!token.equals(slackToken)) {
            log.warn("Received invalid slack token: '{}' in command 'Activate team' from user: '{}'. Returns to " +
                            "slack!",
                    token, fromUser);
            return getRichMessageInvalidSlackCommand();
        }
        log.debug("Start background activate team task");
        backgroundExecutor.execute((() -> backgroundJobController.activateTeam(fromUser, text, responseUrl)));
        //new Thread((() -> backgroundJobService.activateTeam(fromUser, text, responseUrl))).run();
        String response = String.format("Thanks, Activate Team job started!");
        log.info("'Activate team' job started : user: '{}' text: '{}' and sent response into slack: '{}'",
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
