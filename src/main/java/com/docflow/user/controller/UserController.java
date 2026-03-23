package com.docflow.user.controller;

import com.docflow.common.response.ApiResponse;
import com.docflow.common.response.PagedResponse;
import com.docflow.common.security.SecurityUtils;
import com.docflow.document.dto.RecentViewItem;
import com.docflow.stats.service.StatsService;
import com.docflow.user.dto.CreateUserRequest;
import com.docflow.user.dto.CreateUserResponse;
import com.docflow.user.dto.UpdateUserRequest;
import com.docflow.user.dto.UserListItemResponse;
import com.docflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提供目前登入使用者相關 API。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile and history APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final StatsService statsService;
    private final UserService userService;

    /**
     * 取得目前登入使用者最近瀏覽的文件列表。
     *
     * @return 最近瀏覽文件資料
     */
    @Operation(summary = "Get current user's recent viewed documents")
    @GetMapping("/me/recent-views")
    public ApiResponse<List<RecentViewItem>> getMyRecentViews() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(statsService.getRecentViews(userId));
    }

    @Operation(summary = "List users (paged)")
    @GetMapping
    public ApiResponse<PagedResponse<UserListItemResponse>> listUsers(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(userService.getUsers(page, size, keyword));
    }

    @Operation(summary = "Create user")
    @PostMapping
    public ApiResponse<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request), "User created successfully");
    }

    @Operation(summary = "Update user role and status")
    @PutMapping("/{id}")
    public ApiResponse<UserListItemResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateUser(id, request), "User updated successfully");
    }
}
