package com.zakgof.telegram;

import java.util.List;

import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.zakgof.db.velvet.IVelvetEnvironment;
import com.zakgof.db.velvet.entity.Entities;
import com.zakgof.db.velvet.entity.ISetEntityDef;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BotService implements IBotService {

    private final IVelvetEnvironment velvet;

    private static final ISetEntityDef<Long> CHAT = Entities.set(Long.class);

    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");

    @Override
    public void broadcast(String text) {
        List<Long> allChats = getAllChatIds();
        System.err.println("Broadcast message " + text + " to " + allChats);
        for (Long chatId : allChats) {
            try {
                sendMessage(chatId.toString(), text);
            } catch (BotException e) {
                forgetChat(chatId);
            }
        }
    }

    @Override
    public void onUpdate(JsonObject update) {
        if (update.has("message")) {
            JsonObject message = update.getAsJsonObject("message");
            JsonObject chat = message.getAsJsonObject("chat");
            long chatId = chat.get("id").getAsLong();
            if (message.has("text") && message.get("text").getAsString().contains("/unsubscribe")) {
                forgetChat(chatId);
                sendMessage(""+chatId, "Unsubscribed");
            }
            if (message.has("text") && message.get("text").getAsString().contains("/subscribe")) {
                rememberChat(chatId);
                sendMessage(""+chatId, "Subscribed");
            }
        }
    }

    private void sendMessage(String receiver, String text) {
        ImmutableMap<String, String> payload = ImmutableMap.of("chat_id", receiver, "text", text);
        execute("sendMessage", payload);
    }

    private void execute(String command, ImmutableMap<String, String> payload) {
        Gson gson = new Gson();
        String json = gson.toJson(payload);

        try {

            System.err.println("Sending to bot " + json);

            HttpResponse<JsonNode> httpResponse = Unirest.post("https://api.telegram.org/bot" + BOT_TOKEN + "/" + command)
              .header("Content-Type", "application/json")
              .body(json)
              .asJson();

            JSONObject resp = httpResponse.getBody().getObject();

            System.err.println("Bot replied " + resp);

            if (!resp.getBoolean("ok")) {
                String description = resp.getString("description");
                throw new BotException(description);
            }

        } catch (UnirestException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("serial")
    class BotException extends RuntimeException {
        public BotException(String message) {
            super(message);
        }
    }

    private void rememberChat(long chatId) {
        velvet.execute(velvet -> CHAT.put(velvet, chatId));
    }

    private void forgetChat(long chatId) {
        velvet.execute(velvet -> CHAT.deleteKey(velvet, chatId));
    }

    private List<Long> getAllChatIds() {
        return velvet.calculate(velvet -> CHAT.batchGetAllList(velvet));
    }

}
