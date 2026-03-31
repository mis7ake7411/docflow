package com.docflow.user.service;

import com.docflow.common.response.PagedResponse;
import com.docflow.user.dto.CreateUserRequest;
import com.docflow.user.dto.CreateUserResponse;
import com.docflow.user.dto.UpdateUserRequest;
import com.docflow.user.dto.UserListItemResponse;

import java.util.List;

public interface UserService {

    PagedResponse<UserListItemResponse> getUsers(int page, int size, String keyword);

    CreateUserResponse createUser(CreateUserRequest request);

    UserListItemResponse updateUser(Long id, UpdateUserRequest request);

    List<UserListItemResponse> getShareCandidates(String keyword);
}
