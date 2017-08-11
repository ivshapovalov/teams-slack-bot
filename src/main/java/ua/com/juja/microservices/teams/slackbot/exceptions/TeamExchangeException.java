package ua.com.juja.microservices.teams.slackbot.exceptions;

/**
 * @author Danil Kuznetsov
 */
public class TeamExchangeException extends BaseBotException {
    public TeamExchangeException(ApiError error, Exception ex) {
        super(error, ex);
    }
}