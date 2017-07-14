package ua.com.juja.microservices.teams.slackbot.dao.impl;

import org.springframework.stereotype.Repository;
import ua.com.juja.microservices.teams.slackbot.dao.AbstractRestRepository;
import ua.com.juja.microservices.teams.slackbot.dao.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.model.TeamDTO;

import java.util.List;

@Repository
public class RestTeamRepository extends AbstractRestRepository implements TeamRepository {
    @Override
    public String activateTeam(List<String> slackNames) {
        return null;
    }

    @Override
    public String deactivateTeam(String slackName) {
        return null;
    }

    @Override
    public TeamDTO getTeam(String slackName) {
        return null;
    }
}
