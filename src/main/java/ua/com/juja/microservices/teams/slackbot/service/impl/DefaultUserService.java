package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.dao.UserRepository;
import ua.com.juja.microservices.teams.slackbot.model.UserDTO;
import ua.com.juja.microservices.teams.slackbot.service.UserService;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@Service
@Slf4j
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;

    @Inject
    public DefaultUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        Utils.checkNull(slackNames,"SlackNames must not be null!");
        log.debug("Received slackNames: '{}' to search in User repo", slackNames.toString());
        List<UserDTO> users = userRepository.findUsersBySlackNames(slackNames);
        log.debug("Found users '{}' by slackNames '{}' in User repo", users.toString(), slackNames.toString());
        log.info("Found users '{}' in User repo", users.toString());
        return users;
    }

    @Override
    public List<UserDTO> findUsersByUuids(List<String> uuids) {
        Utils.checkNull(uuids,"Uuids must not be null!");
        log.debug("Received uuids: '{}' to search in User repo", uuids.toString());
        List<UserDTO> users = userRepository.findUsersByUuids(uuids);
        log.debug("Found users '{}' by uuids '{}' in User repo", users.toString(), uuids.toString());
        log.info("Found users '{}' in User repo", users.toString());
        return users;
    }
}
