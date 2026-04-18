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

    public static String userServiceUrl()    { return props.getProperty("user.service.url"); }
    public static String auctionServiceUrl() { return props.getProperty("auction.service.url"); }
    public static String biddingServiceUrl() { return props.getProperty("bidding.service.url"); }
    public static String paymentServiceUrl() { return props.getProperty("payment.service.url"); }
    public static String websocketUrl()      { return props.getProperty("websocket.url"); }
}