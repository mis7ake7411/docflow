package com.docflow.user.service;

import com.docflow.common.exception.BadRequestException;
import com.docflow.common.response.PagedResponse;
import com.docflow.user.dto.CreateUserRequest;
import com.docflow.user.dto.CreateUserResponse;
import com.docflow.user.dto.UpdateMyProfileRequest;
import com.docflow.user.dto.UpdateUserRequest;
import com.docflow.user.dto.UserListItemResponse;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import com.docflow.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

/**
 * {@link UserService} 的預設實作，負責使用者帳戶建立與管理。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final int TEMP_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分頁查詢使用者，支援按使用者名稱或電郵搜尋。
     *
     * @param page 頁碼（從 0 開始）
     * @param size 每頁筆數
     * @param keyword 搜尋關鍵字（可為空）
     * @return 分頁使用者清單
     */
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserListItemResponse> getUsers(int page, int size, String keyword) {
        log.debug("Querying users: page={}, size={}, keyword={}", page, size, keyword != null ? keyword : "");
        PageRequest pageable = PageRequest.of(page, size);
        Page<User> users;
        if (keyword == null || keyword.isBlank()) {
            log.debug("Loading all users");
            users = userRepository.findAll(pageable);
        } else {
            log.debug("Searching users by keyword");
            String term = keyword.trim();
            users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable);
        }

        log.debug("User query completed: totalElements={}, totalPages={}", users.getTotalElements(), users.getTotalPages());
        return PagedResponse.<UserListItemResponse>builder()
                .items(users.map(this::toListItem).toList())
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .build();
    }

    /**
     * 建立新使用者並產生臨時密碼。
     *
     * @param request 建立使用者的請求資料
     * @return 建立後的使用者資訊及臨時密碼
     * @throws BadRequestException 若使用者名稱或電郵已存在
     */
    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        log.info("Creating user: username={}, email={}", request.getUsername(), request.getEmail());
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("User creation rejected due to duplicate username: {}", request.getUsername());
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User creation rejected due to duplicate email: {}", request.getEmail());
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
        log.info("User created successfully: userId={}, username={}, role={}", saved.getId(), saved.getUsername(), role);

        return CreateUserResponse.builder()
                .user(toListItem(saved))
                .tempPassword(tempPassword)
                .build();
    }

    /**
     * 更新使用者電子郵件、角色與狀態。
     *
     * @param id 使用者編號
     * @param request 更新請求資料
     * @return 更新後的使用者資訊
     * @throws BadRequestException 若使用者不存在
     */
    @Override
    @Transactional
    public UserListItemResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user: userId={}, email={}, role={}, status={}", id, request.getEmail(), request.getRole(), request.getStatus());
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String normalizedEmail = normalizeEmail(request.getEmail());
        validateEmailUniqueness(normalizedEmail, id);

        user.setEmail(normalizedEmail);
        user.setRole(parseRole(request.getRole()));
        user.setStatus(parseStatus(request.getStatus()));

        User saved = userRepository.save(user);
        log.info("User updated successfully: userId={}, username={}", saved.getId(), saved.getUsername());
        return toListItem(saved);
    }

    @Override
    @Transactional
    public UserListItemResponse updateMyProfile(UpdateMyProfileRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating current user profile: userId={}, email={}", currentUserId, request.getEmail());

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String normalizedEmail = normalizeEmail(request.getEmail());
        validateEmailUniqueness(normalizedEmail, currentUserId);
        user.setEmail(normalizedEmail);

        User saved = userRepository.save(user);
        log.info("Current user profile updated successfully: userId={}", saved.getId());
        return toListItem(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserListItemResponse> getShareCandidates(String keyword) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Loading share candidates: currentUserId={}", currentUserId);
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        return userRepository.findShareCandidates(UserStatus.ACTIVE, currentUserId, normalizedKeyword).stream()
                .map(this::toListItem)
                .toList();
    }

    /**
     * 將使用者實體轉換為列表項回應物件。
     *
     * @param user 使用者實體
     * @return 使用者列表項資訊
     */
    private UserListItemResponse toListItem(User user) {
        log.trace("Converting user entity to list item: userId={}, username={}", user.getId(), user.getUsername());
        return UserListItemResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 將角色字串轉換為列舉型別。
     *
     * @param role 角色字串
     * @return 使用者角色列舉
     * @throws BadRequestException 若角色值無效
     */
    private UserRole parseRole(String role) {
        log.debug("Parsing role: role={}", role);
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            log.warn("Invalid role provided: {}", role);
            throw new BadRequestException("Invalid role");
        }
    }

    /**
     * 將狀態字串轉換為列舉型別。
     *
     * @param status 狀態字串
     * @return 使用者狀態列舉
     * @throws BadRequestException 若狀態值無效
     */
    private UserStatus parseStatus(String status) {
        log.debug("Parsing status: status={}", status);
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            log.warn("Invalid status provided: {}", status);
            throw new BadRequestException("Invalid status");
        }
    }

    private void validateEmailUniqueness(String email, Long userId) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            log.warn("User update rejected due to duplicate email: email={}, userId={}", email, userId);
            throw new BadRequestException("Email already exists");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim();
    }

    /**
     * 產生隨機臨時密碼。
     * 密碼至少包含一個字母、一個數字，並由字母與數字組成。
     *
     * @return 隨機臨時密碼
     */
    private String generateTempPassword() {
        log.debug("Generating temporary password");
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
        log.debug("Temporary password generated successfully");
        return new String(chars);
    }
}
