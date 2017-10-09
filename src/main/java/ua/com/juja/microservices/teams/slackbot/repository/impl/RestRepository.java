package ua.com.juja.microservices.teams.slackbot.repository.impl;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Ivan Shapovalov
 */
public class RestRepository {

    protected final EurekaClient eurekaClient;
    @Value("${gateway.name}")
    private String gatewayName;

    public RestRepository(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    protected String discovery(String endPointUrl) {
        Application gateway = eurekaClient.getApplication(gatewayName);
        InstanceInfo gatewayInfo = gateway.getInstances().get(0);
        String gatewayHost = gatewayInfo.getHostName();
        int gatewayPort = gatewayInfo.getPort();
        String gatewayFullUrl = "http://" + gatewayHost + ":" + gatewayPort + endPointUrl;
        return gatewayFullUrl;
    }
}