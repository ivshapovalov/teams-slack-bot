package ua.com.juja.microservices.teams.slackbot.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;
import ua.com.juja.microservices.teams.slackbot.model.User;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceImplTest {

    @Inject
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    public void findUsersBySlackNamesExecutedCorrectly() throws Exception {
        //given
        List<String> slackNamesRequest = Arrays.asList("@user11", "@user22");
        List<User> expected = Arrays.asList(new User("uuid1", "@user11"),
                new User("uuid2", "user2"));
        given(userRepository.findUsersBySlackNames(slackNamesRequest)).willReturn(expected);
        //when
        List<User> actual = userService.findUsersBySlackNames(slackNamesRequest);
        //then
        assertThat(actual, is(expected));
        verify(userRepository).findUsersBySlackNames(slackNamesRequest);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    public void findUsersByUuidsExecutedCorrectly() throws Exception {
        //given
        List<String> uuidsRequest = Arrays.asList("uuid1", "uuid2");
        List<User> expected = Arrays.asList(new User("uuid1", "@user11"),
                new User("uuid2", "user2"));
        given(userRepository.findUsersByUuids(uuidsRequest)).willReturn(expected);
        //when
        List<User> actual = userService.findUsersByUuids(uuidsRequest);
        //then
        assertThat(actual, is(expected));
        verify(userRepository).findUsersByUuids(uuidsRequest);
        verifyNoMoreInteractions(userRepository);
    }
}