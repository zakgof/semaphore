package com.zakgof.telegram;

import com.google.gson.JsonObject;

public interface IBotService {

    void broadcast(String string);

    void onUpdate(JsonObject bodyElement);


}
