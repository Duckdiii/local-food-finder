package com.example.cuisine_finder.utils;

import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.UserRepository;
import java.util.ArrayList;
import java.util.List;

public class FirebaseUtils {
    public static void pushMockUsers() {
        UserRepository userRepository = new UserRepository();
        List<User> mockUsers = new ArrayList<>();

        User user1 = new User();
        user1.setId("mock_user_1");
        user1.setFullName("Nguyễn Văn An");
        user1.setEmail("an.nguyen@example.com");
        user1.setRole("USER");
        user1.setActive(true);
        user1.setCreatedAt(System.currentTimeMillis());
        mockUsers.add(user1);

        User admin = new User();
        admin.setId("mock_admin_1");
        admin.setFullName("Admin Cuisine");
        admin.setEmail("admin@cuisinefinder.com");
        admin.setRole("ADMIN");
        admin.setActive(true);
        admin.setCreatedAt(System.currentTimeMillis());
        mockUsers.add(admin);

        for (User user : mockUsers) {
            userRepository.saveUser(user);
        }
    }
}
