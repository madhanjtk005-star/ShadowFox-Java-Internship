package com.shadowfox.leavemanagement.service;

import com.shadowfox.leavemanagement.dto.CreateUserRequest;
import com.shadowfox.leavemanagement.dto.UserResponse;
import com.shadowfox.leavemanagement.entity.Role;
import com.shadowfox.leavemanagement.entity.User;
import com.shadowfox.leavemanagement.exception.BusinessValidationException;
import com.shadowfox.leavemanagement.exception.ForbiddenException;
import com.shadowfox.leavemanagement.exception.ResourceNotFoundException;
import com.shadowfox.leavemanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    /** Loads the user and throws 403 unless they are an ADMIN. */
    @Transactional(readOnly = true)
    public User requireAdmin(Long id) {
        User user = getById(id);
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only ADMIN users can perform this action");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long id) {
        return UserResponse.from(getById(id));
    }

    @Transactional
    public UserResponse createUser(Long adminId, CreateUserRequest req) {
        requireAdmin(adminId);
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessValidationException("A user with email " + req.email() + " already exists");
        }
        User saved = userRepository.save(new User(req.name(), req.email(), req.role(), req.leaveBalance()));
        return UserResponse.from(saved);
    }
}
