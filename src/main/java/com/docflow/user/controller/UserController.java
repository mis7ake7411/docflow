package com.docflow.user.controller;

import com.docflow.common.response.ApiResponse;
import com.docflow.common.response.PagedResponse;
import com.docflow.common.security.SecurityUtils;
import com.docflow.document.dto.RecentViewItem;
import com.docflow.stats.service.StatsService;
import com.docflow.user.dto.CreateUserRequest;
import com.docflow.user.dto.CreateUserResponse;
import com.docflow.user.dto.UpdateMyProfileRequest;
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

    /**
     * 更新目前登入使用者的個人資料。
     *
     * @param request 更新資料
     * @return 更新後的使用者摘要
     */
    @Operation(summary = "Update current user profile")
    @PutMapping("/me/profile")
    public ApiResponse<UserListItemResponse> updateMyProfile(@Valid @RequestBody UpdateMyProfileRequest request) {
        return ApiResponse.success(userService.updateMyProfile(request), "Profile updated successfully");
    }

    /**
     * 取得可分享對象候選清單。
     *
     * @param keyword 搜尋關鍵字，可為空
     * @return 分享候選使用者清單
     */
    @Operation(summary = "Get share candidates")
    @GetMapping("/share-candidates")
    public ApiResponse<List<UserListItemResponse>> getShareCandidates(
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(userService.getShareCandidates(keyword));
    }

    /**
     * 分頁取得使用者列表。
     *
     * @param page 頁碼（從 0 開始）
     * @param size 每頁筆數
     * @param keyword 搜尋關鍵字，可為空
     * @return 分頁使用者資料
     */
    @Operation(summary = "List users (paged)")
    @GetMapping
    public ApiResponse<PagedResponse<UserListItemResponse>> listUsers(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(userService.getUsers(page, size, keyword));
    }

    /**
     * 建立新使用者。
     *
     * @param request 建立使用者請求資料
     * @return 建立完成的使用者資料與臨時密碼
     */
    @Operation(summary = "Create user")
    @PostMapping
    public ApiResponse<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request), "User created successfully");
    }

    /**
     * 更新指定使用者的電子郵件、角色與狀態。
     *
     * @param id 使用者編號
     * @param request 更新使用者請求資料
     * @return 更新後的使用者資料
     */
    @Operation(summary = "Update user profile, role and status")
    @PutMapping("/{id}")
    public ApiResponse<UserListItemResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateUser(id, request), "User updated successfully");
    }
}
