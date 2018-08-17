package com.zakgof.semaphore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.amazonaws.services.kinesis.model.InvalidArgumentException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zakgof.db.velvet.IVelvetEnvironment;
import com.zakgof.db.velvet.VelvetFactory;
import com.zakgof.semaphore.model.Resource;
import com.zakgof.semaphore.service.AuthService;
import com.zakgof.semaphore.service.IAuthService;
import com.zakgof.semaphore.service.ISemaphoreService;
import com.zakgof.semaphore.service.SemaphoreServiceImpl;
import com.zakgof.telegram.BotService;
import com.zakgof.telegram.IBotService;

public class Lambda implements RequestHandler<Object, Map<String, ?>> {

    private final ISemaphoreService semaphoreService;
    private final IAuthService authService;
    private final IBotService botService;
    private final Router router;

    public Lambda() {
        this("velvetdb://dynamodb/us-east-1?x=y");
    }

    private Lambda(String velvetUrl) {
        IVelvetEnvironment velvetEnvironment = VelvetFactory.open(velvetUrl);
        this.authService = new AuthService();
        this.botService = new BotService(velvetEnvironment);
        this.semaphoreService = new SemaphoreServiceImpl(velvetEnvironment, authService, botService);

        this.router = new Router();

        router.register("get", data -> ImmutableMap.<String, Object>of("resources", semaphoreService.loadAll()));

        router.register("resource.add",    data -> empty(() -> semaphoreService.addResource(dejson(Resource.class, data))));
        router.register("resource.delete", data -> empty(() -> semaphoreService.deleteResource(str(data, "resourceId"))));

        router.register("resource.lock", data -> empty(() -> semaphoreService.requestLock(str(data, "jwt"),
                                                                                          str(data, "resourceId"),
                                                                                          lng(data, "minutes"))));

        router.register("resource.release", data -> empty(() ->semaphoreService.releaseLock(str(data, "jwt"),
                                                                                            str(data, "resourceId"))));

        router.register("login", data -> ImmutableMap.<String, Object>of("jwt", authService.login(
                                                                       lng(data, "id"),
                                                                       str(data, "first_name"),
                                                                       str(data, "last_name"),
                                                                       str(data, "username"),
                                                                       str(data, "auth_date"),
                                                                       str(data, "hash"))));
    }


    private static String str(JsonElement data, String fieldname) {
        JsonElement element = data.getAsJsonObject().get(fieldname);
        return element == null ? null : element.getAsString();
    }

    private static Long lng(JsonElement data, String fieldname) {
        JsonElement element = data.getAsJsonObject().get(fieldname);
        return element == null ? null : element.getAsLong();
    }

    private Map<String, ?> empty(Runnable runnable) {
        try {
            runnable.run();
            return Collections.emptyMap();
        } catch (BusinessException e) {
            return ImmutableMap.of("error", e.getMessage());
        }

    }

    @Override
    public Map<String, ?> handleRequest(Object input, Context context) {

        @SuppressWarnings("unchecked")
        Map<String, ?> inputMap = (Map<String, ?>) input;
        String httpMethod  = inputMap.get("httpMethod").toString();

        if (httpMethod.equals("OPTIONS")) {
            return ImmutableMap.of(
                  "isBase64Encoded", false,
                  "statusCode", 200,
                  "headers", ImmutableMap.of("Access-Control-Allow-Origin", "*"),
                  "body", "");

        }

        String inputBodyJson = inputMap.get("body").toString();

        int statusCode = 200;
        String bodyText = "";
        try {
            bodyText = process(httpMethod, inputBodyJson);
        } catch (Exception e) {
            statusCode = 500;
            bodyText = flattenException(e);
        }
        return ImmutableMap.of(
            "isBase64Encoded", false,
            "statusCode", statusCode,
            "headers", ImmutableMap.of("Access-Control-Allow-Origin", "*"),
            "body", bodyText);
    }

    private String process(String httpMethod, String inputBodyJson) {
        Gson gson = new Gson();
        JsonObject bodyElement = gson.fromJson(inputBodyJson, JsonElement.class).getAsJsonObject();
        System.err.println("Received body " + bodyElement);
        if (bodyElement.has("update_id")) {
            botService.onUpdate(bodyElement);
            return "";
        }
        String command = bodyElement.get("command").getAsString();
        JsonElement dataElement = bodyElement.getAsJsonObject().get("data");
        Object response = router.route(command, dataElement);
        return gson.toJson(response);
    }

    private static <T> T dejson(Class<T> clazz, JsonElement data) {
        return new Gson().fromJson(data, clazz);
    }

    private static String flattenException(Exception e) {
        String bodyText;
        StringWriter strOut = new StringWriter();
        e.printStackTrace(new PrintWriter(strOut));
        bodyText = strOut.toString();
        return bodyText;
    }

    private static class Router {
        private final Map<String, Function <JsonElement, Map<String, ?>>> postMap = new HashMap<>();

        void register(String command, Function<JsonElement, Map<String, ?>> handler) {
            postMap.put(command, handler);
        }

        Map<String, ?> route(String command, JsonElement data) {
            Function<JsonElement, Map<String, ?>> function = postMap.get(command);
            if (function == null) {
                throw new InvalidArgumentException("No route for POST command: " + command);
            }
            return function.apply(data);
        }

    }

    public static void main(String[] args) {
        // for debug
    }



}
