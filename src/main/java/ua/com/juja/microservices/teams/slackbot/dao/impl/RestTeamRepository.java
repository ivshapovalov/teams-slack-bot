package ua.com.juja.microservices.teams.slackbot.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ua.com.juja.microservices.teams.slackbot.dao.TeamRepository;
import ua.com.juja.microservices.teams.slackbot.model.Team;

import java.util.List;

@Repository
@Slf4j
public class RestTeamRepository extends AbstractRestRepository implements TeamRepository {
    @Override
    public Team activateTeam(List<String> slackNames) {
        return null;
    }

    @Override
    public Team deactivateTeam(String slackName) {
        return null;
    }

    @Override
    public Team getTeam(String slackName) {
        return null;
    }
}
