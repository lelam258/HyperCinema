package com.cinema.hyperCinema.service;

public interface EmailService {

    void sendActivationCode(String to, String code);

    void sendForgotPasswordCode(String to, String code);
}
