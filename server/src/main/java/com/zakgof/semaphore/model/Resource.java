package com.zakgof.semaphore.model;

import com.zakgof.db.velvet.annotation.Key;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Resource {
    @Key
    private final String id;
    private final String descr;
}
