package ua.com.juja.microservices.teams.slackbot.exceptions;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.com.juja.microservices.teams.slackbot.util.Util;

@RestControllerAdvice
@Slf4j
public class ExceptionsHandler {

    private String responseUrl;

    public void setResponseUrl(String responseUrl) {
        this.responseUrl = responseUrl;
    }

    @ExceptionHandler(Exception.class)
    public void handleAllOtherExceptions(Exception ex) {
        log.warn("Other Exception': {}", ex.getMessage());
        Util.sendResultRichMessage(responseUrl,new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(WrongCommandFormatException.class)
    public void handleWrongCommandFormatException(Exception ex) {
        log.warn("WrongCommandFormatException: {}", ex.getMessage());
        Util.sendResultRichMessage(responseUrl,new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(UserExchangeException.class)
    public void handleUserExchangeException(UserExchangeException ex) {
        log.warn("UserExchangeException: {}", ex.detailMessage());
        Util.sendResultRichMessage(responseUrl,new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(TeamExchangeException.class)
    public void handleTeamExchangeException(TeamExchangeException ex) {
        String message = ex.getMessage();
        if (ex.getError() != null && ex.getError().getExceptionMessage().contains("#")) {
            message = ex.getError().getExceptionMessage();
        }
        log.warn("TeamExchangeException : {}", ex.detailMessage());
        Util.sendResultRichMessage(responseUrl,new RichMessage(message));
    }
}