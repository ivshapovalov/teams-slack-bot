package ua.com.juja.microservices.teams.slackbot.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ivan Shapovalov
 */
public class SlackUserHandlerTest {

    @Test
    public void getSlackUsersFromTextWhenTextIsEmpty() {
        String text = "";
        Set<String> expected = new HashSet<>();

        Assert.assertEquals(expected, SlackUserHandler.getSlackUsersFromText(text));
    }

    @Test
    public void getSlackUsersFromTextWhenFullNameInText() {
        String text = " <@id|ivan> Hello <";
        Set<String> expected = new HashSet<>(Collections.singletonList("id"));

        Assert.assertEquals(expected, SlackUserHandler.getSlackUsersFromText(text));
    }

    @Test
    public void getSlackUsersFromTextWhenNameInText() {
        String text = " <@id> Hello >";
        Set<String> expected = new HashSet<>(Collections.singletonList("id"));

        Assert.assertEquals(expected, SlackUserHandler.getSlackUsersFromText(text));
    }

    @Test
    public void getSlackUsersFromTextWhenTwoNamesInText() {
        String text = " <@id1|ivan> Hello <@id2|alex> afda <> adfa <";
        Set<String> expected = new HashSet<>(Arrays.asList("id1", "id2"));

        Assert.assertEquals(expected, SlackUserHandler.getSlackUsersFromText(text));
    }
}
