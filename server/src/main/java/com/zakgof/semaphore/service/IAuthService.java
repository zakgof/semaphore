package com.zakgof.semaphore.service;

import com.zakgof.semaphore.model.User;

public interface IAuthService {

    String login(Long id, String firstName, String lastName, String username, String authDate, String hash);

    User parseUser(String gwt);

}
