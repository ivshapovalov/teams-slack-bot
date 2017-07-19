package ua.com.juja.microservices.teams.slackbot.exceptions;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
}