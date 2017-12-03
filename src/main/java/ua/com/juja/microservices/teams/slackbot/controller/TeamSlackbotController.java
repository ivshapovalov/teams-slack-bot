package ua.com.juja.microservices.teams.slackbot.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping(value = "v1/commands")
public class TeamSlackbotController {

    private final RestTemplate restTemplate;
    private final TeamService teamService;
    private final ExceptionsHandler exceptionsHandler;

    @Value("${slack.slashCommandToken}")
    private String slackToken;
    @Value("${message.sorry}")
    private String SORRY_MESSAGE;
    @Value("${message.activate.team.instant}")
    private String ACTIVATE_TEAM_INSTANT_MESSAGE;
    @Value("${message.activate.team.delayed}")
    private String ACTIVATE_TEAM_DELAYED_MESSAGE;
    @Value("${message.get.team.instant}")
    private String GET_TEAM_INSTANT_MESSAGE;
    @Value("${message.get.team.delayed}")
    private String GET_TEAM_DELAYED_MESSAGE;
    @Value("${message.get.my.team.instant}")
    private String GET_MY_TEAM_INSTANT_MESSAGE;
    @Value("${message.get.my.team.delayed}")
    private String GET_MY_TEAM_DELAYED_MESSAGE;
    @Value("${message.deactivate.team.instant}")
    private String DEACTIVATE_TEAM_INSTANT_MESSAGE;
    @Value("${message.deactivate.team.delayed}")
    private String DEACTIVATE_TEAM_DELAYED_MESSAGE;

    @Inject
    public TeamSlackbotController(TeamService teamService,
                                  ExceptionsHandler exceptionsHandler,
                                  RestTemplate restTemplate) {
        this.teamService = teamService;
        this.exceptionsHandler = exceptionsHandler;
        this.restTemplate = restTemplate;
    }

    @PostMapping(value = "/teams/activate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ApiOperation(
            value = "Activate new Team, consist of four users",
            notes = "This method activates new Team, which contains four users"
    )
    @ApiResponses(value = {
            @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Successfully activated Team"),
            @ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad request"),
            @ApiResponse(code = HttpURLConnection.HTTP_BAD_METHOD, message = "Bad method"),
            @ApiResponse(code = HttpURLConnection.HTTP_UNSUPPORTED_TYPE, message = "Unsupported request media type")
    })
    public void onReceiveSlashCommandActivateTeam(@RequestParam("token") String token,
                                                  @RequestParam("user_name") String fromUser,
                                                  @RequestParam("text") String text,
                                                  @RequestParam("response_url") String responseUrl,
                                                  HttpServletResponse response) throws IOException {
        exceptionsHandler.setResponseUrl(responseUrl);
        if (isRequestCorrect(token, response, fromUser, responseUrl)) {
            sendInstantResponseMessage(response, ACTIVATE_TEAM_INSTANT_MESSAGE);
            teamService.activateTeam(fromUser, text);
            RichMessage message = new RichMessage(String.format(ACTIVATE_TEAM_DELAYED_MESSAGE, text));
            sendDelayedResponseMessage(responseUrl, message);
            log.info("'Activate team' command processed : fromUser: '{}', text: '{}', response_url: '{}' and sent " +
                    "message to slack: '{}'", fromUser, text, responseUrl, message.getText());
        }
    }

    @PostMapping(value = "/teams/deactivate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ApiOperation(
            value = "Deactivate Team",
            notes = "This method deactivates Team"
    )
    @ApiResponses(value = {
            @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Successfully deactivated Team"),
            @ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad request"),
            @ApiResponse(code = HttpURLConnection.HTTP_BAD_METHOD, message = "Bad method"),
            @ApiResponse(code = HttpURLConnection.HTTP_UNSUPPORTED_TYPE, message = "Unsupported request media type")
    })
    public void onReceiveSlashCommandDeactivateTeam(@RequestParam("token") String token,
                                                    @RequestParam("user_name") String fromUser,
                                                    @RequestParam("text") String text,
                                                    @RequestParam("response_url") String responseUrl,
                                                    HttpServletResponse response) throws IOException {
        exceptionsHandler.setResponseUrl(responseUrl);
        if (isRequestCorrect(token, response, fromUser, responseUrl)) {
            sendInstantResponseMessage(response, String.format(DEACTIVATE_TEAM_INSTANT_MESSAGE, text));
            Set<String> slackNames = teamService.deactivateTeam(fromUser, text);
            RichMessage message = new RichMessage(String.format(DEACTIVATE_TEAM_DELAYED_MESSAGE,
                    slackNames.stream().sorted().collect(Collectors.joining(" "))));
            sendDelayedResponseMessage(responseUrl, message);
            log.info("'Deactivate team' command processed : fromUser: '{}', text: '{}', response_url: '{}' and sent " +
                    "message to slack: '{}'", fromUser, text, responseUrl, message.getText());
        }
    }

    @PostMapping(value = "/teams", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ApiOperation(
            value = "Get members of active Team of certain user",
            notes = "This method get members of active Team of certain user"
    )
    @ApiResponses(value = {
            @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Successfully got members of active Team of certain user"),
            @ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad request"),
            @ApiResponse(code = HttpURLConnection.HTTP_BAD_METHOD, message = "Bad method"),
            @ApiResponse(code = HttpURLConnection.HTTP_UNSUPPORTED_TYPE, message = "Unsupported request media type")
    })
    public void onReceiveSlashCommandGetTeam(@RequestParam("token") String token,
                                             @RequestParam("user_name") String fromUser,
                                             @RequestParam("text") String text,
                                             @RequestParam("response_url") String responseUrl,
                                             HttpServletResponse response) throws IOException {
        exceptionsHandler.setResponseUrl(responseUrl);
        if (isRequestCorrect(token, response, fromUser, responseUrl)) {
            sendInstantResponseMessage(response, String.format(GET_TEAM_INSTANT_MESSAGE, text));
            Set<String> slackNames = teamService.getTeam(text);
            RichMessage message = new RichMessage(String.format(GET_TEAM_DELAYED_MESSAGE,
                    text, slackNames.stream().sorted().collect(Collectors.joining(" "))));
            sendDelayedResponseMessage(responseUrl, message);
            log.info("'Get team' command processed : fromUser: '{}', text: '{}', response_url: '{}' and sent " +
                    "message to slack: '{}'", fromUser, text, responseUrl, message.getText());
        }
    }

    @PostMapping(value = "/myteam", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ApiOperation(
            value = "Get members of my active Team",
            notes = "This method get members of my active Team"
    )
    @ApiResponses(value = {
            @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Successfully got members of my active Team"),
            @ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad request"),
            @ApiResponse(code = HttpURLConnection.HTTP_BAD_METHOD, message = "Bad method"),
            @ApiResponse(code = HttpURLConnection.HTTP_UNSUPPORTED_TYPE, message = "Unsupported request media type")
    })
    public void onReceiveSlashCommandGetMyTeam(@RequestParam("token") String token,
                                               @RequestParam("user_name") String fromUser,
                                               @RequestParam("response_url") String responseUrl,
                                               HttpServletResponse response) throws IOException {
        exceptionsHandler.setResponseUrl(responseUrl);
        if (isRequestCorrect(token, response, fromUser, responseUrl)) {
            fromUser = fromUser.startsWith("@") ? fromUser : "@" + fromUser;
            sendInstantResponseMessage(response, String.format(GET_MY_TEAM_INSTANT_MESSAGE, fromUser));
            Set<String> slackNames = teamService.getTeam(fromUser);
            RichMessage message = new RichMessage(String.format(GET_MY_TEAM_DELAYED_MESSAGE,
                    fromUser, slackNames.stream().sorted().collect(Collectors.joining(" "))));
            sendDelayedResponseMessage(responseUrl, message);
            log.info("'Get my team' command processed : fromUser: '{}', text: '{}', response_url: '{}' and sent " +
                    "message to slack: '{}'", fromUser, responseUrl, message.getText());
        }
    }

    private void sendInstantResponseMessage(HttpServletResponse response, String message) throws IOException {
        log.debug("Before sending instant response message '{}' ", message);
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter printWriter = response.getWriter();
        printWriter.print(message);
        printWriter.flush();
        printWriter.close();
        log.info("After sending instant response message to slack '{}' ", message);
    }

    private void sendDelayedResponseMessage(String responseUrl, RichMessage message) {
        log.debug("Before sending delayed response message '{}' to slack response_url '{}' ", message.getText(),
                responseUrl);
        String response = restTemplate.postForObject(responseUrl, message, String.class);
        log.debug("After sending delayed response message. Response is '{}'", response);
    }

    private boolean isRequestCorrect(String token, HttpServletResponse response, String... params)
            throws IOException {
        log.debug("Before checking parameters of request from slack. Token '{}', other '{}' ", token,
                Arrays.stream(params).sorted().collect(Collectors.joining(",")));
        if (!token.equals(slackToken) ||
                Arrays.stream(params).filter(param -> param == null || param.isEmpty()).collect(Collectors.toList()).size() > 0) {
            sendInstantResponseMessage(response, SORRY_MESSAGE);
            return false;
        }
        log.debug("After checking parameters of request from slack. Parameters is correct");
        return true;
    }
}
