package com.zakgof.semaphore.model;

import com.zakgof.db.velvet.annotation.Key;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class User {
    @Key
    private final Long telegramId;
    private final String displayName;

    @Override
    public String toString() {
        return "[" + displayName + "/" + telegramId + "]";
    }
}
