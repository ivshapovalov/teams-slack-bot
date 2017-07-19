package ua.com.juja.microservices.teams.slackbot.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import ua.com.juja.microservices.teams.slackbot.dao.UserRepository;
import ua.com.juja.microservices.teams.slackbot.model.UserDTO;

import java.util.List;

@Repository
@Slf4j
public class RestUserRepository extends AbstractRestRepository implements UserRepository {

    @Override
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        return null;
    }
}
