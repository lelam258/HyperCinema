package com.cinema.hyperCinema.service;

import com.cinema.hyperCinema.dto.auth.RegisterRequestDTO;
import com.cinema.hyperCinema.dto.auth.ResetPasswordRequestDTO;
import com.cinema.hyperCinema.model.User;

public interface AuthService {

    User registerCustomer(RegisterRequestDTO dto);

    void requestForgotPassword(String email);

    void resetPassword(ResetPasswordRequestDTO dto);

    void activateAccount(String email, String code);
}
