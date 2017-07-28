package ua.com.juja.microservices.teams.slackbot.exceptions;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.com.juja.microservices.teams.slackbot.service.impl.SlackNameHandlerService;
import ua.com.juja.microservices.teams.slackbot.util.Util;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ivan Shapovalov
 */
@RestControllerAdvice
@Slf4j
public class ExceptionsHandler {

    private SlackNameHandlerService slackNameHandlerService;
    private String responseUrl;

    @Inject
    public ExceptionsHandler(SlackNameHandlerService slackNameHandlerService) {
        this.slackNameHandlerService = slackNameHandlerService;
    }

    public void setResponseUrl(String responseUrl) {
        this.responseUrl = responseUrl;
    }

    @ExceptionHandler(Exception.class)
    public void handleAllOtherExceptions(Exception ex) {
        log.warn("Other Exception': {}", ex.getMessage());
        Util.sendPostResponseAsRichMessage(responseUrl, new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(WrongCommandFormatException.class)
    public void handleWrongCommandFormatException(Exception ex) {
        log.warn("WrongCommandFormatException: {}", ex.getMessage());
        Util.sendPostResponseAsRichMessage(responseUrl, new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(UserExchangeException.class)
    public void handleUserExchangeException(UserExchangeException ex) {
        log.warn("UserExchangeException: {}", ex.detailMessage());
        Util.sendPostResponseAsRichMessage(responseUrl, new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(TeamExchangeException.class)
    public void handleTeamExchangeException(TeamExchangeException ex) {
        String message = ex.getMessage();
        if (ex.getError() != null && ex.getError().getExceptionMessage().contains("#")) {
            message = replaceUuidsBySlackNamesInMessage(ex.getError().getExceptionMessage());
        }
        log.warn("TeamExchangeException : {}", ex.detailMessage());
        Util.sendPostResponseAsRichMessage(responseUrl, new RichMessage(message));
    }

    private String replaceUuidsBySlackNamesInMessage(String message) {
        log.debug("Start find and replace uuids by slackNames in message {}", message);
        String[] array = message.split("#");
        if (array.length > 1) {
            Set<String> uuids = new HashSet<>(Arrays.asList(array[1].split(",")));
            Set<String> slackNames = slackNameHandlerService.convertUuidsToSlackNames(uuids);
            array[1] = slackNames.stream().collect(Collectors.joining(","));
            message = Arrays.asList(array).stream().collect(Collectors.joining(""));
        }
        log.debug("Finished find and replace uuids by slackNames in message {}", message);
        return message;
    }
}