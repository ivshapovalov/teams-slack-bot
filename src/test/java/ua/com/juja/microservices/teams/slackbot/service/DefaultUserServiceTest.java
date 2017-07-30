package ua.com.juja.microservices.teams.slackbot.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.teams.slackbot.dao.UserRepository;
import ua.com.juja.microservices.teams.slackbot.model.UserDTO;

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
public class DefaultUserServiceTest {

    @Inject
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    public void findUsersBySlackNamesExecutedCorrectly() throws Exception {
        //given
        List<String> slackNamesRequest = Arrays.asList(new String[]{"@user11", "@user22"});
        List<UserDTO> expected = Arrays.asList(new UserDTO[]{new UserDTO("uuid1", "@user11"),
                new UserDTO("uuid2", "user2")});
        given(userRepository.findUsersBySlackNames(slackNamesRequest)).willReturn(expected);
        //when
        List<UserDTO> actual = userService.findUsersBySlackNames(slackNamesRequest);
        //then
        assertThat(actual, is(expected));
        verify(userRepository).findUsersBySlackNames(slackNamesRequest);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    public void findUsersByUuidsExecutedCorrectly() throws Exception {
        //given
        List<String> uuidsRequest = Arrays.asList(new String[]{"uuid1", "uuid2"});
        List<UserDTO> expected = Arrays.asList(new UserDTO[]{new UserDTO("uuid1", "@user11"),
                new UserDTO("uuid2", "user2")});
        given(userRepository.findUsersByUuids(uuidsRequest)).willReturn(expected);
        //when
        List<UserDTO> actual = userService.findUsersByUuids(uuidsRequest);
        //then
        assertThat(actual, is(expected));
        verify(userRepository).findUsersByUuids(uuidsRequest);
        verifyNoMoreInteractions(userRepository);
    }
}