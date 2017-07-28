package ua.com.juja.microservices.teams.slackbot.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import ua.com.juja.microservices.teams.slackbot.dao.UserRepository;
import ua.com.juja.microservices.teams.slackbot.model.DTO.UserDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivan Shapovalov
 */
@Repository
@Qualifier("fake")
@Slf4j
public class FakeUserRepository extends AbstractRestRepository implements UserRepository {

    private static final Map<String, String> users = new HashMap<>();

    static {
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
        users.put("12", "@l");    }

    @Override
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        log.debug("Received slackNames to search : '{}' in Fake repo", slackNames);
        for (int i = 0; i < slackNames.size(); i++) {
            if (!slackNames.get(i).startsWith("@")) {
                log.debug("add '@' to SlackName : [{}]", slackNames.get(i));
                String slackName = slackNames.get(i);
                slackNames.set(i, "@" + slackName);
            }
        }
        List<UserDTO> users = FakeUserRepository.users.entrySet().stream()
                .filter(user -> slackNames.contains(user.getValue()))
                .map(user -> new UserDTO(user.getKey(), user.getValue()))
                .collect(Collectors.toList());
        log.debug("Finished searching in Fake Users service. Users is: [{}]", users.toString());
        log.info("Found '{}' users in Fake repo by slackNames", users.size());
        return users;
    }

    @Override
    public List<UserDTO> findUsersByUuids(List<String> uuids) {
        log.debug("Received uuids to convert : '{}'", uuids);
        List<UserDTO> result = users.entrySet().stream()
                .filter(user -> uuids.contains(user.getKey()))
                .map(user -> new UserDTO(user.getKey(), user.getValue()))
                .collect(Collectors.toList());
        log.debug("Finished request to Fake Users service. Response is: '{}'", result.toString());
        log.info("Found UserDTO: '{}' by uuids '{}'", result, uuids);
        return result;
    }
}
