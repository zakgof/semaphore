package com.zakgof.semaphore.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Lock {

    private final LocalDateTime start;
    private final LocalDateTime release;

    public Lock setRelease(LocalDateTime release) {
        return new Lock(this.start, release);
    }
}
