package com.campus.frontend.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class
                .getResourceAsStream("/config.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load config.properties", e);
        }
    }

    private static String resolveProperty(String key) {
        String value = props.getProperty(key);
        if (value != null && value.contains("${SERVICE_HOST_IP}")) {
            String hostIp = System.getenv("SERVICE_HOST_IP");
            if (hostIp == null || hostIp.isEmpty()) {
                hostIp = "localhost";
            }
            value = value.replace("${SERVICE_HOST_IP}", hostIp);
        }
        return value;
    }

    public static String userServiceUrl()    { return resolveProperty("user.service.url"); }
    public static String auctionServiceUrl() { return resolveProperty("auction.service.url"); }
    public static String biddingServiceUrl() { return resolveProperty("bidding.service.url"); }
    public static String paymentServiceUrl() { return resolveProperty("payment.service.url"); }
    public static String websocketUrl()      { return resolveProperty("websocket.url"); }
}