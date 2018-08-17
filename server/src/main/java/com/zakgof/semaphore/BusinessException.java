package com.zakgof.semaphore;

@SuppressWarnings("serial")
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
