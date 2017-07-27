package ua.com.juja.microservices.teams.slackbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.teams.slackbot.dao.UserRepository;
import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;
import ua.com.juja.microservices.teams.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Artem
 */
@Service
@Slf4j
public class DefaultUserService implements UserService {

    @Qualifier ("fake")
    private final UserRepository userRepository;

    @Inject
    public DefaultUserService(@Qualifier ("fake") UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        log.debug("Received slackNames: '{}' for convert", slackNames.toString());
        List<UserDTO> users = userRepository.findUsersBySlackNames(slackNames);
        log.info("Found users '{}' by slackNames {}", users.toString(), slackNames.toString());
        return users;
    }

    @Override
    public List<UserDTO> findUsersByUuids(List<String> uuids) {
        log.debug("Received uuids: '{}' for convert", uuids.toString());
        List<UserDTO> users = userRepository.findUsersByUuids(uuids);
        log.info("Found users '{}' by uuids {}", users.toString(), uuids.toString());
        return users;
    }
}
