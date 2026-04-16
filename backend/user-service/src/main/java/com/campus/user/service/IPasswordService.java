package com.campus.user.service;

public interface IPasswordService {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
