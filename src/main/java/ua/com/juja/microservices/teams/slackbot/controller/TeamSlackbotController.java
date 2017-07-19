package ua.com.juja.microservices.teams.slackbot.controller;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1")
@Slf4j
public class TeamSlackbotController {

    @PostMapping(value = "/commands/teams/activate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommandCreateTeam(@RequestParam("token") String token,
                                                       @RequestParam("user_name") String fromUser,
                                                       @RequestParam("text") String text) {
        //TODO Should be implemented feature SLB-F1
        return null;
    }

    @PostMapping(value = "/commands/teams/deactivate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommandDeactivateTeam(@RequestParam("token") String token,
                                                                 @RequestParam("user_name") String fromUser,
                                                                 @RequestParam("text") String text) {
        //TODO Should be implemented feature SLB-F2
        return null;
    }

    @PostMapping(value = "/commands/team", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommandGetTeam(@RequestParam("token") String token,
                                                              @RequestParam("user_name") String fromUser,
                                                              @RequestParam("text") String text) {
        //TODO Should be implemented feature SLB-F3
        return null;
    }

    @PostMapping(value = "/commands/myteam", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RichMessage onReceiveSlashCommandGetMyTeam(@RequestParam("token") String token,
                                                            @RequestParam("user_name") String fromUser,
                                                            @RequestParam("text") String text) {
        //TODO Should be implemented feature SLB-F4
        return null;
    }

}
