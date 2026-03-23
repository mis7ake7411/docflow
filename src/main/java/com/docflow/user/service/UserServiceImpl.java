package com.docflow.user.service;

import com.docflow.common.exception.BadRequestException;
import com.docflow.common.response.PagedResponse;
import com.docflow.user.dto.CreateUserRequest;
import com.docflow.user.dto.CreateUserResponse;
import com.docflow.user.dto.UpdateUserRequest;
import com.docflow.user.dto.UserListItemResponse;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final int TEMP_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserListItemResponse> getUsers(int page, int size, String keyword) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<User> users;
        if (keyword == null || keyword.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            String term = keyword.trim();
            users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable);
        }

        return PagedResponse.<UserListItemResponse>builder()
                .items(users.map(this::toListItem).toList())
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        UserRole role = parseRole(request.getRole());
        UserStatus status = parseStatus(request.getStatus());
        String tempPassword = generateTempPassword();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(role)
                .status(status)
                .mustChangePassword(true)
                .build();

        User saved = userRepository.save(user);

        return CreateUserResponse.builder()
                .user(toListItem(saved))
                .tempPassword(tempPassword)
                .build();
    }

    @Override
    @Transactional
    public UserListItemResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setRole(parseRole(request.getRole()));
        user.setStatus(parseStatus(request.getStatus()));

        User saved = userRepository.save(user);
        return toListItem(saved);
    }

    private UserListItemResponse toListItem(User user) {
        return UserListItemResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid role");
        }
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid status");
        }
    }

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        char[] chars = new char[TEMP_PASSWORD_LENGTH];
        chars[0] = LETTERS.charAt(random.nextInt(LETTERS.length()));
        chars[1] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        for (int i = 2; i < TEMP_PASSWORD_LENGTH; i++) {
            String pool = LETTERS + DIGITS;
            chars[i] = pool.charAt(random.nextInt(pool.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
