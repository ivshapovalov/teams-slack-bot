package ua.com.juja.microservices.teams.slackbot.repository.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
public abstract class RestRepository {

    protected final DiscoveryClient discoveryClient;
    @Value("${gateway.name}")
    private String gatewayName;

    public RestRepository(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    protected String getCommandGatewayUrl(String endPointUrl) {
        List<ServiceInstance> instances = discoveryClient.getInstances(gatewayName);
        if (instances == null || instances.size() == 0) {
            throw new RuntimeException(String.format("Eureka may be not contain %s instance", gatewayName));
        }
        ServiceInstance gatewayInfo = instances.get(0);
        String gatewayHost = gatewayInfo.getHost();
        int gatewayPort = gatewayInfo.getPort();
        return "http://" + gatewayHost + ":" + gatewayPort + endPointUrl;
    }
}