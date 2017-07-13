package ua.com.juja.microservices.teams.slackbot.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import ua.com.juja.microservices.teams.slackbot.model.UserDTO;

import java.util.List;

@Repository
public class RestUserRepository implements UserRepository{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        return null;
    }
}
