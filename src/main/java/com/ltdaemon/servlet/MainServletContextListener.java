package com.ltdaemon.servlet;


import com.ltdaemon.requesting.RequestBot;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Properties;

public class MainServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

//        Properties systemProperties = System.getProperties();
//        systemProperties.setProperty("java.net.useSystemProxies", "true");
//        systemProperties.setProperty("http.proxyHost", PROXY_ADDRESS);
//        systemProperties.setProperty("http.proxyPort", PROXY_PORT);
//        System.setProperties(systemProperties);


        ApiContextInitializer.init();

        TelegramBotsApi botsApi = new TelegramBotsApi();

        try {
            botsApi.registerBot(new RequestBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }

}
