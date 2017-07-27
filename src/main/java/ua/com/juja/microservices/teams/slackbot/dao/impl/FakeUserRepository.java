package ua.com.juja.microservices.teams.slackbot.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.dao.UserRepository;
import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Qualifier("fake")
@Slf4j
public class FakeUserRepository extends AbstractRestRepository implements UserRepository {

    private static final Map<String, String> users = new HashMap<>();

    static {
        populateUsers();
    }

    private final RestTemplate restTemplate;

    @Inject
    public FakeUserRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static void populateUsers() {
        users.put("1", "@a");
        users.put("100", "@ivan.shapovalov");
        users.put("2", "@b");
        users.put("3", "@c");
        users.put("4", "@d");
        users.put("5", "@e");
        users.put("6", "@f");
        users.put("7", "@g");
        users.put("8", "@h");
        users.put("9", "@i");
        users.put("10", "@j");
        users.put("11", "@k");
        users.put("12", "@l");
    }

    @Override
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        log.debug("Received slackNames to convert : '{}'", slackNames);
        for (int i = 0; i < slackNames.size(); i++) {
            if (!slackNames.get(i).startsWith("@")) {
                log.debug("add '@' to SlackName : [{}]", slackNames.get(i));
                String slackName = slackNames.get(i);
                slackNames.set(i, "@" + slackName);
            }
        }
        List<UserDTO> result = new ArrayList<>();
        result = users.entrySet().stream()
                .filter(user -> slackNames.contains(user.getValue()))
                .map(user -> new UserDTO(user.getKey(), user.getValue()))
                .collect(Collectors.toList());
        log.debug("Finished request to Fake Users service. Response is: [{}]", result.toString());
        log.info("Got UserDTO:{} by slackNames: {}", result, slackNames);
        return result;
    }

    @Override
    public List<UserDTO> findUsersByUuids(List<String> uuids) {
        log.debug("Received uuids to convert : '{}'", uuids);
        List<UserDTO> result = new ArrayList<>();
        result = users.entrySet().stream()
                .filter(user -> uuids.contains(user.getKey()))
                .map(user -> new UserDTO(user.getKey(), user.getValue()))
                .collect(Collectors.toList());
        log.debug("Finished request to Fake Users service. Response is: [{}]", result.toString());
        log.info("Got UserDTO:{} by uuids: {}", result, uuids);
        return result;
    }
}
