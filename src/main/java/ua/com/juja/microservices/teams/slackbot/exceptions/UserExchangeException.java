package ua.com.juja.microservices.teams.slackbot.exceptions;

/**
 * @author Danil Kuznetsov
 */
public class UserExchangeException extends BaseBotException {

    public UserExchangeException(ApiError error, Exception ex) {
        super(error, ex);
    }
}