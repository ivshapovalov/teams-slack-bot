package ua.com.juja.microservices.teams.slackbot.exceptions;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ExceptionsHandler {
    @ExceptionHandler(Exception.class)
    public RichMessage handleAllOtherExceptions(Exception ex) {
        log.warn("Other Exception': {}", ex.getMessage());
        return new RichMessage(ex.getMessage());
    }

    @ExceptionHandler(WrongCommandFormatException.class)
    public RichMessage handleWrongCommandFormatException(Exception ex) {
        log.warn("WrongCommandFormatException: {}", ex.getMessage());
        return new RichMessage(ex.getMessage());
    }

    @ExceptionHandler(UserExchangeException.class)
    public RichMessage handleUserExchangeException(UserExchangeException ex) {
        log.warn("UserExchangeException: {}", ex.detailMessage());
        return new RichMessage(ex.getMessage());
    }

    @ExceptionHandler(TeamExchangeException.class)
    public RichMessage handleTeamExchangeException(TeamExchangeException ex) {
        log.warn("TeamExchangeException : {}", ex.detailMessage());
        return new RichMessage(ex.getMessage());
    }
}