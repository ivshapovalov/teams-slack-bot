package ua.com.juja.microservices.teams.slackbot.repository.feign;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ua.com.juja.microservices.teams.slackbot.model.users.User;
import ua.com.juja.microservices.teams.slackbot.model.users.UserSlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.users.UserUuidRequest;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@FeignClient(name = "gateway")
public interface UsersClient {
    @RequestMapping(method = RequestMethod.POST, value = "/v1/users/usersBySlackNames", consumes =
            MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    List<User> findUsersBySlackNames(UserSlackNameRequest request);

    @RequestMapping(method = RequestMethod.POST, value = "/v1/users/usersByUuids", consumes =
            MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    List<User> findUsersByUuids(UserUuidRequest request);
}
