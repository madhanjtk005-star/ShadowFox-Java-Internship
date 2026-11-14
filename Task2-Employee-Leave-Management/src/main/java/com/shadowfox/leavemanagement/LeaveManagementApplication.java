package com.shadowfox.leavemanagement;

import com.shadowfox.leavemanagement.entity.Role;
import com.shadowfox.leavemanagement.entity.User;
import com.shadowfox.leavemanagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LeaveManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveManagementApplication.class, args);
    }

    /** Seeds one admin and two employees on first run so the API can be tried immediately. */
    @Bean
    CommandLineRunner seedUsers(UserRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new User("Alice Admin", "alice.admin@company.com", Role.ADMIN, 24));
                repo.save(new User("Bob Employee", "bob@company.com", Role.EMPLOYEE, 20));
                repo.save(new User("Carol Employee", "carol@company.com", Role.EMPLOYEE, 15));
            }
        };
    }
}
