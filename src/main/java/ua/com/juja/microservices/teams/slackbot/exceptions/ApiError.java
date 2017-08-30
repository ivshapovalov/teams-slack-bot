package ua.com.juja.microservices.teams.slackbot.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Getter
@ToString
public class ApiError {
    /**
     * The status is duplicate http httpStatus internalErrorCode
     */
    private int httpStatus;
    /**
     * The code is internal error code for this exception
     */
    private String internalErrorCode;
    /**
     * The messages for user
     */
    private String clientMessage;
    /**
     * The messages  for developer
     */
    private String developerMessage;
    /**
     * The messages  in exception
     */
    private String exceptionMessage;
    /**
     * List of detail error messages
     */
    private List<String> detailErrors;
}