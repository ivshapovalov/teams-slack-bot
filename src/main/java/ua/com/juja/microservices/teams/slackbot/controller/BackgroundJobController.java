package ua.com.juja.microservices.teams.slackbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.BaseBotException;
import ua.com.juja.microservices.teams.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.teams.slackbot.model.Team;
import ua.com.juja.microservices.teams.slackbot.model.TeamRequest;
import ua.com.juja.microservices.teams.slackbot.service.TeamSlackbotService;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ivan Shapovalov
 */
@Slf4j
@Controller
public class BackgroundJobController {

    private RestTemplate restTemplate;
    private SlackNameHandlerService slackNameHandlerService;
    private TeamSlackbotService teamSlackbotService;

    @Inject
    public BackgroundJobController(RestTemplate restTemplate, TeamSlackbotService teamSlackbotService,
                                   SlackNameHandlerService slackNameHandlerService) {
        this.restTemplate = restTemplate;
        this.teamSlackbotService = teamSlackbotService;
        this.slackNameHandlerService = slackNameHandlerService;
    }

    public void activateTeam(String fromUser, String text, String responseUrl) {
//        try {
            log.debug("Started background Activate team job for user '{}' text: '{}' response_url: '{}'",
                    fromUser, text, responseUrl);

            log.debug("Started create slackParsedCommand from user '{}' and text '{}'", fromUser, text);
            SlackParsedCommand slackParsedCommand = slackNameHandlerService.createSlackParsedCommand(fromUser, text);
            log.debug("Finished create slackParsedCommand");

            log.debug("Started create TeamRequest");
            TeamRequest teamRequest = new TeamRequest(slackParsedCommand);
            log.debug("Finished create TeamRequest");

            log.debug("Send activate team request to Teams service. Team: '{}'", teamRequest.toString());
            Team activatedTeam = teamSlackbotService.activateTeam(teamRequest);
            log.debug("Received response from Teams service: '{}'", activatedTeam.toString());

            log.debug("Started createUsersSetFromUuids for team '{}' ", activatedTeam.toString());
            Set<String> slackNames = slackNameHandlerService.createUsersSetFromUuids(activatedTeam.getMembers());
            log.debug("Finished createUsersSetFromUuids for team '{}'. slacknames is '{}' ", activatedTeam.toString()
                    , slackNames);

            String message = String.format("Thanks, new Team for users '%s' activated",
                    slackNames.stream().collect(Collectors.joining(",")));

            sendDelayedResponceToSlack(responseUrl, message);

            log.info("'Activate team' command processed : user: '{}' text: '{}' and sent response into slack: '{}'",
                    fromUser, text, message);
//        } catch (BaseBotException e) {
//            log.warn("Exception: {}", e);
//            String message = e.getError().getExceptionMessage();
//            message = replaceUuidsBySlackNames(message);
//            //sendDelayedResponceToSlack(responseUrl, message);
//        } catch (Exception e) {
//            //sendDelayedResponceToSlack(responseUrl, e.getMessage());
//        }
    }

    private String replaceUuidsBySlackNames(String message) {
        log.debug("Start find and replace uuids by slackNames in message {}",message);
        String[] array = message.split("#");
        if (array.length > 1) {
            Set<String> uuids = new HashSet<>(Arrays.asList(array[1].split(",")));
            Set<String> slackNames = slackNameHandlerService.createUsersSetFromUuids(uuids);
            array[1] = slackNames.stream().collect(Collectors.joining(","));
            message=Arrays.asList(array).stream().collect(Collectors.joining(""));
        }
        log.debug("Finished find and replace uuids by slackNames in message {}",message);
        return message;
    }

    private void sendDelayedResponceToSlack(String responseUrl, String message) {
        try {
            Map<String, String> params = new HashMap();
            params.put("text", message);

            HttpEntity<?> request = new HttpEntity<>(params, setupBaseHttpHeaders());
            log.debug("Started send delayed response to slack '{}'. URL is : '{}'", message, responseUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(responseUrl,
                    request, String.class);
            log.debug("Finished send delayed response to slack. Slack response is: '{}'", response.toString());
        } catch (HttpClientErrorException ex) {
            ApiError error = convertToApiError(ex);
            log.warn("Slack service returned an error: '{}'", error);
            throw new BaseBotException(error, ex);
        }
    }

    protected HttpHeaders setupBaseHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return headers;
    }

    protected ApiError convertToApiError(HttpClientErrorException httpClientErrorException) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(httpClientErrorException.getResponseBodyAsString(), ApiError.class);
        } catch (IOException e) {
            return new ApiError(
                    500, "BotInternalError",
                    "I'm, sorry. I cannot parse api error message from remote service :(",
                    "Cannot parse api error message from remote service",
                    e.getMessage(),
                    Arrays.asList(httpClientErrorException.getMessage())
            );
        }
    }
}
