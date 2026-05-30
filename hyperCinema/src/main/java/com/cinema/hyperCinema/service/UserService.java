package com.cinema.hyperCinema.service;

import com.cinema.hyperCinema.model.User;
import java.util.List;
import java.util.Map;

public interface UserService {
    List<User> getAllUsers();
    List<User> searchAndFilterUsers(String query, String roleName, String status);
    User getUserById(Integer id);
    User createUser(User user);
    User updateUser(Integer id, User userDetails);
    void deleteUser(Integer id);
    User toggleUserStatus(Integer id);
    User assignUserRole(Integer id, Integer roleId);
    User resetUserPassword(Integer id, String newPassword);
    Map<String, Long> getUserStatistics();
}
