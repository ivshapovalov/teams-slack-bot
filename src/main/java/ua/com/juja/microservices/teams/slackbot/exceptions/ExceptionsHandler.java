package ua.com.juja.microservices.teams.slackbot.exceptions;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.service.UserService;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ivan Shapovalov
 */
@RestControllerAdvice
@Slf4j
public class ExceptionsHandler {

    private final RestTemplate restTemplate;

    private final UserService userService;

    private String responseUrl;

    @Inject
    public ExceptionsHandler(RestTemplate restTemplate, UserService userService) {
        this.restTemplate = restTemplate;
        this.userService = userService;
    }

    public void setResponseUrl(String responseUrl) {
        this.responseUrl = responseUrl;
    }

    @ExceptionHandler(Exception.class)
    public void handleAllOtherExceptions(Exception ex) {
        log.warn("Other Exception': {}", ex.getMessage());
        sendPostResponseAsRichMessage(new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(WrongCommandFormatException.class)
    public void handleWrongCommandFormatException(Exception ex) {
        log.warn("WrongCommandFormatException: {}", ex.getMessage());
        sendPostResponseAsRichMessage(new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(UserExchangeException.class)
    public void handleUserExchangeException(UserExchangeException ex) {
        log.warn("UserExchangeException: {}", ex.detailMessage());
        sendPostResponseAsRichMessage(new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(TeamExchangeException.class)
    public void handleTeamExchangeException(TeamExchangeException ex) {
        log.warn("TeamExchangeException : {}", ex.detailMessage());
        String message = ex.getMessage();
        ApiError apiError = ex.getError();
        if (apiError != null && apiError.getExceptionMessage().contains("#")) {
            message = replaceUuidsBySlackNamesInMessage(apiError.getExceptionMessage());
        }
        sendPostResponseAsRichMessage(new RichMessage(message));
    }

    private String replaceUuidsBySlackNamesInMessage(String message) {
        log.debug("Start find and replace uuids by slackNames in message {}", message);
        String[] messageParts = message.split("#");
        if (messageParts.length > 1) {
            Set<String> uuids = new HashSet<>(Arrays.asList(messageParts[1].split(",")));
            Set<String> slackNames = new HashSet<>();
            try {
                List<User> users = userService.findUsersByUuids(new ArrayList<>(uuids));
                slackNames = users.stream().map(User::getSlack)
                        .collect(Collectors.toSet());
            } catch (Exception ex) {
                log.warn("Nested exception : '{}'", ex.getMessage());
            }
            messageParts[1] = Utils.listToStringWithDelimeter(slackNames, ",");
            message = Utils.arrayToStringWithDelimeter(messageParts, "");
        }
        log.debug("Finished find and replace uuids by slackNames in message {}", message);
        return message;
    }

    private void sendPostResponseAsRichMessage(RichMessage richMessage) {
        try {
            restTemplate.postForObject(responseUrl, richMessage, String.class);
        } catch (Exception ex) {
            log.warn("Nested exception : '{}' with text '{}' . Unable to send response to slack", ex.getMessage(),
                    richMessage.getText());
        }
    }

}