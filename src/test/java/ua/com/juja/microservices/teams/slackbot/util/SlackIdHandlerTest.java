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
public class SlackIdHandlerTest {
    @Test
    public void getSlackIdsFromTextWhenTextIsEmpty() {
        String text = "";
        Set<String> expected = new HashSet<>();
        Assert.assertEquals(expected, SlackIdHandler.getSlackIdsFromText(text));
    }

    @Test
    public void getSlackIdsFromTextWhenFullNameInText() {
        String text = " <@id|ivan> Hello <";
        Set<String> expected = new HashSet<>(Collections.singletonList("id"));
        Assert.assertEquals(expected, SlackIdHandler.getSlackIdsFromText(text));
    }

    @Test
    public void getSlackIdsFromTextWhenNameInText() {
        String text = " <@id> Hello >";
        Set<String> expected = new HashSet<>(Collections.singletonList("id"));
        Assert.assertEquals(expected, SlackIdHandler.getSlackIdsFromText(text));
    }

    @Test
    public void getSlackIdsFromTextWhenTwoNamesInText() {
        String text = " <@id1|ivan> Hello <@id2|alex> afda <> adfa <";
        Set<String> expected = new HashSet<>(Arrays.asList("id1", "id2"));
        Assert.assertEquals(expected, SlackIdHandler.getSlackIdsFromText(text));
    }
}
