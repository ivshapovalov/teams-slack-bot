package ua.com.juja.microservices.teams.slackbot.repository.feign;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackIdRequest;
import ua.com.juja.microservices.teams.slackbot.model.users.UserUuidRequest;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@FeignClient(name = "gateway")
public interface UsersClient {
    @RequestMapping(method = RequestMethod.POST, value = "/v1/users/usersBySlackIds", consumes =
            MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    List<User> findUsersBySlackIds(UserSlackIdRequest request);

    @RequestMapping(method = RequestMethod.POST, value = "/v1/users/usersByUuids", consumes =
            MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    List<User> findUsersByUuids(UserUuidRequest request);
}
