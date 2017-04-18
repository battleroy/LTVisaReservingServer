package com.ltdaemon.requesting;


import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class RequestBot extends TelegramLongPollingBot {

    private static final String POLL_AVAILABLE_URL = "https://evas2.urm.lt/calendar/json?_d=&_aby=3&_cry=6&_c=1&_b=2";
    private static final String POLL_RESERVED_URL = "https://evas2.urm.lt/calendar/json?_d=&_aby=3&_cry=6&_c=1&_b=1";

    private static final String C_POLL_AVAILABLE_DATES_NOW = "/pollavailable";
    private static final String C_POLL_RESERVED_DATES_NOW = "/pollreserved";
    private static final String C_POLL_SUBSCRIBE = "/subscribe";
    private static final String C_POLL_UNSUBSCRIBE = "/unsubscribe";


    private static final Map<String, String> pollUrlsByCommand = new HashMap<String, String>() {{
        put(C_POLL_AVAILABLE_DATES_NOW, POLL_AVAILABLE_URL);
        put(C_POLL_RESERVED_DATES_NOW, POLL_RESERVED_URL);
    }};

    private static final String S_TRY_OTHER_COMMAND = "Bitch, you missed right command.";
    private static final String S_NO_DATES = "There are no dates for request.";
    private static final String S_POLL_ERROR = "Poll error occurred.";
    private static final String S_SUBSCRIBED = "Subscribed.";
    private static final String S_UNSUBSCRIBED = "Unsubscribed.";


//        put("46.251.49.21", 8080); BEST EVER PROXY
//        put("88.119.137.155", 1080);
//        put("78.63.16.100", 1080);
//        put("91.187.180.13", 36555);
//        put("81.23.6.236", 3128);
//        put("88.119.150.143", 8080);
//        put("89.190.101.177", 21320);
//        put("77.79.35.98", 8080);
//        put("217.117.28.132", 80);
//        put("77.221.81.106", 8080);
//        put("77.221.67.219", 8080);
//        put("86.100.118.44", 80);
//        put("85.204.74.103", 3128);
//        put("88.119.204.46", 8080);

    private static HttpTransport HTTP_TRANSPORT;
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    static {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("46.251.49.21", 8080));
        HTTP_TRANSPORT = new NetHttpTransport.Builder().setProxy(proxy).build();
    }

    private static final long POLL_RATE_MS = 5 * 60 * 1000;
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private final Set<Long> subscribedChatIds = new HashSet<>();
    private final HttpRequestFactory requestFactory;


    public RequestBot() {
        super();

        requestFactory = HTTP_TRANSPORT.createRequestFactory(
                httpRequest -> {
                    httpRequest.setConnectTimeout(2 * 60 * 1000);
                    httpRequest.setReadTimeout(2 * 60 * 1000);
                    httpRequest.getHeaders().set("X-Requested-With", "XMLHttpRequest");
                    httpRequest.getHeaders().setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36");
                    httpRequest.setParser(new JsonObjectParser(JSON_FACTORY));
                }
        );

        service.scheduleAtFixedRate(() -> {
            System.out.println("Scheduled poll fired.");

            List<String> availableDates = pollAvailableDates(pollUrlsByCommand.get(C_POLL_AVAILABLE_DATES_NOW));
            if (availableDates != null && availableDates.size() > 0) {
                synchronized (subscribedChatIds) {
                    String pollResultText = getPollAvailableResponseString(availableDates);

                    for (Long chatId : subscribedChatIds) {
                        respondToChatWithIdWithText(chatId, pollResultText);
                    }
                }
            }

        }, 5 * 1000, POLL_RATE_MS, TimeUnit.MILLISECONDS);
    }


    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String messageText = message.getText();

            if (messageText.equals(C_POLL_AVAILABLE_DATES_NOW) ||
                    messageText.equals(C_POLL_RESERVED_DATES_NOW)) {

                handleImmediatePolls(message, messageText);

            } else if (messageText.equals(C_POLL_SUBSCRIBE) ||
                    messageText.equals(C_POLL_UNSUBSCRIBE)) {

                handleSubscription(message, messageText.equals(C_POLL_SUBSCRIBE));

            } else {
                respondToMessageWithText(message, S_TRY_OTHER_COMMAND);
            }
        }

    }

    @Override
    public String getBotUsername() {
        return "lt_visa_reserving_bot";
    }

    @Override
    public String getBotToken() {
        return "346326872:AAGUwQG6Jxe-zzQnCpxJ6ILKXEvlFZYeL08";
    }


    private void handleImmediatePolls(Message message, String command) {
        List<String> pollResults = pollAvailableDates(pollUrlsByCommand.get(command));
        respondToMessageWithText(message, getPollAvailableResponseString(pollResults));
    }


    private void handleSubscription(Message message, boolean isSubscribing) {
        synchronized (subscribedChatIds) {
            Long chatId = message.getChatId();

            if (isSubscribing) {
                subscribedChatIds.add(chatId);
            } else {
                subscribedChatIds.remove(chatId);
            }

            respondToMessageWithText(message, isSubscribing ? S_SUBSCRIBED : S_UNSUBSCRIBED);
        }
    }


    private void respondToMessageWithText(Message message, String text) {
        respondToChatWithIdWithText(message.getChatId(), text);
    }


    private void respondToChatWithIdWithText(Long chatId, String text) {
        SendMessage responseMessage = new SendMessage() // Create a SendMessage object with mandatory fields
                .setChatId(chatId)
                .setText(text);
        try {
            sendMessage(responseMessage); // Call method to send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private String getPollAvailableResponseString(List<String> pollResults) {
        StringBuilder sb = new StringBuilder();

        if (pollResults == null) {
            sb.append(S_POLL_ERROR);
        } else if (pollResults.size() == 0) {
            sb.append(S_NO_DATES);
        } else {
            for (String result : pollResults) {
                sb.append(String.format("Result date: %s\n", result));
            }
        }

        return sb.toString();
    }


    @SuppressWarnings("unchecked")
    private List<String> pollAvailableDates(String pollUrl) {
        try {
            System.out.println("Polling started...");

            GenericUrl url = new GenericUrl(pollUrl);
            HttpRequest request = requestFactory.buildGetRequest(url);

            List objectList = request.execute().parseAs(List.class);
            List<String> result = new ArrayList<>();

            boolean isError = false;

            for (Object obj : objectList) {
                if (obj instanceof String) {
                    String dateString = (String) obj;

                    if (dateString.length() > 0) {
                        result.add((String) obj);
                    }
                } else {
                    isError = true;
                    break;
                }
            }

            if (!isError) {
                System.out.println("Poll succeeded.");

                return result;
            }

            return null;
        } catch (IOException e) {
            System.err.println("Poll error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
