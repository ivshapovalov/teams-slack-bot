package ua.com.juja.microservices.teams.slackbot.exceptions;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.service.UserService;

import javax.inject.Inject;

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
        sendErrorResponseAsRichMessage(new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(WrongCommandFormatException.class)
    public void handleWrongCommandFormatException(Exception ex) {
        log.warn("WrongCommandFormatException: {}", ex.getMessage());
        sendErrorResponseAsRichMessage(new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(UserExchangeException.class)
    public void handleUserExchangeException(UserExchangeException ex) {
        log.warn("UserExchangeException: {}", ex.detailMessage());
        sendErrorResponseAsRichMessage(new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(TeamExchangeException.class)
    public void handleTeamExchangeException(TeamExchangeException ex) {
        log.warn("TeamExchangeException : {}", ex.detailMessage());
        String message = ex.getMessage();
        ApiError apiError = ex.getError();
        if (apiError != null && apiError.getExceptionMessage().contains("#")) {
            try {
                message = userService.replaceUuidsBySlackNamesInExceptionMessage(apiError.getExceptionMessage());
            } catch (Exception e) {
                log.warn("Nested exception : '{}'", e.getMessage());
            }
        }
        sendErrorResponseAsRichMessage(new RichMessage(message));
    }

    private void sendErrorResponseAsRichMessage(RichMessage richMessage) {
        try {
            restTemplate.postForObject(responseUrl, richMessage, String.class);
        } catch (Exception ex) {
            log.warn("Nested exception : '{}' with text '{}' . Unable to send response to slack", ex.getMessage(),
                    richMessage.getText());
        }
    }

}