package ua.com.juja.microservices.teams.slackbot.exceptions;

import lombok.extern.slf4j.Slf4j;
import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
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

    private ThreadLocal<String> responseUrl=new ThreadLocal<>();

    @Inject
    public ExceptionsHandler(RestTemplate restTemplate, UserService userService) {
        this.restTemplate = restTemplate;
        this.userService = userService;
    }

    public void setResponseUrl(String responseUrl) {
        this.responseUrl.set(responseUrl);
    }

    @ExceptionHandler(Exception.class)
    public void handleAllOtherExceptions(Exception ex) {
        sendErrorResponseAsRichMessage(new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(WrongCommandFormatException.class)
    public void handleWrongCommandFormatException(Exception ex) {
        sendErrorResponseAsRichMessage(new RichMessage(ex.getMessage()));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public void handleResourceAccessException(ResourceAccessException ex) {
        sendErrorResponseAsRichMessage(new RichMessage("Some service unavailable"));
    }

    @ExceptionHandler(UserExchangeException.class)
    public void handleUserExchangeException(UserExchangeException ex) {
        sendErrorResponseAsRichMessage(new RichMessage(ex.getExceptionMessage()));
    }

    @ExceptionHandler(TeamExchangeException.class)
    public void handleTeamExchangeException(TeamExchangeException ex) {
        String message = ex.getMessage();
        String exceptionMessage= ex.getExceptionMessage();
        if (exceptionMessage != null && exceptionMessage.contains("#")) {
            try {
                message = userService.replaceUuidsBySlackIdsInExceptionMessage(exceptionMessage);
            } catch (Exception e) {
                log.warn("Nested exception : '{}'", e.getMessage());
            }
        }
        sendErrorResponseAsRichMessage(new RichMessage(message));
    }

    private void sendErrorResponseAsRichMessage(RichMessage richMessage) {
        log.debug("Before sending error response message '{}' to slack response_url '{}' ", richMessage.getText(),
                responseUrl.get());
        try {
            restTemplate.postForObject(responseUrl.get(), richMessage, String.class);
        } catch (Exception ex) {
            log.warn("Nested exception : '{}' with text '{}' . Unable to send response to slack", ex.getMessage(),
                    richMessage.getText());
        }
        log.debug("After sending error response message '{}' to slack response_url '{}' ", richMessage.getText(),
                responseUrl.get());
    }
}