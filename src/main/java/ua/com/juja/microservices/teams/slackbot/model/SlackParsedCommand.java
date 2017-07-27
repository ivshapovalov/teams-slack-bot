package ua.com.juja.microservices.teams.slackbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import ua.com.juja.microservices.teams.slackbot.exceptions.WrongCommandFormatException;
import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivan Shapovalov
 */
@ToString(exclude = {"SLACK_NAME_PATTERN"})
@Slf4j
public class SlackParsedCommand {
    private final String SLACK_NAME_PATTERN = "@([a-zA-z0-9\\.\\_\\-]){1,21}";
    private String fromSlackName;
    private String text;
    private List<String> slackNamesInText;
    private int userCountInText;
    private Map<String, UserDTO> users;

    public SlackParsedCommand(String fromSlackName, String text, Map<String, UserDTO> users) {

        if (!fromSlackName.startsWith("@")) {
            log.debug("add '@' to slack name [{}]", fromSlackName);
            fromSlackName = "@" + fromSlackName;
        }
        this.fromSlackName = fromSlackName;
        this.text = text;
        this.slackNamesInText = receiveAllSlackNames(text);
        this.users = users;
        this.userCountInText = slackNamesInText.size();
    }

    public List<String> getSlackNamesInText() {
        return slackNamesInText;
    }

    public String getText() {
        return text;
    }

    public UserDTO getFromUser() {
        return users.get(fromSlackName);
    }

    public UserDTO getFirstUser() {
        checkIsTextContainsSlackName();
        UserDTO result = users.get(slackNamesInText.get(0));
        log.debug("Found the user: {} in the text: [{}]", result.toString(), text);
        return result;
    }

    public int getUserCountInText() {
        return userCountInText;
    }

    private List<String> receiveAllSlackNames(String text) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(SLACK_NAME_PATTERN);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    public Set<UserDTO> getTeamMembers() {
        checkIsTextContainsSlackName();
        Set<UserDTO> result = new LinkedHashSet<>(users.values());
        result.remove(result.stream().filter(res -> res.getSlack().equals(fromSlackName)).findFirst().get());
        log.debug("Found {} users in the text: [{}]", result.size(), text);
        return result;
    }

    public void checkIsTextContainsSlackName() {
        if (userCountInText == 0) {
            log.warn("The text: [{}] doesn't contain slack name.");
            throw new WrongCommandFormatException(String.format("The text '%s' doesn't contains slackName", text));
        }
    }


    private List<Token> receiveTokensWithPositionInText(String[] tokens) {
        Set<Token> result = new TreeSet<>();
        for (String token : tokens) {
            if (!text.contains(token)) {
                throw new WrongCommandFormatException(String.format("Token '%s' didn't find in the string '%s'",
                        token, text));
            }
            int tokenCounts = text.split(token).length - 1;
            if (tokenCounts > 1) {
                throw new WrongCommandFormatException(String.format("The text '%s' contains %d tokens '%s', " +
                        "but expected 1", text, tokenCounts, token));
            }
            result.add(new Token(token, text.indexOf(token)));
        }
        return new ArrayList<>(result);
    }

    @AllArgsConstructor
    @Getter
    class Token implements Comparable {
        private String token;
        private int positionInText;

        @Override
        public int compareTo(Object object) {
            Token thatToken = (Token) object;
            return positionInText - thatToken.getPositionInText();
        }
    }
}
