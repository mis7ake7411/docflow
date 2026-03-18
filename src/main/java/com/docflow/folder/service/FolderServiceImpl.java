package com.docflow.folder.service;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.security.SecurityUtils;
import com.docflow.folder.dto.CreateFolderRequest;
import com.docflow.folder.dto.FolderResponse;
import com.docflow.folder.dto.FolderTreeResponse;
import com.docflow.folder.dto.UpdateFolderRequest;
import com.docflow.folder.entity.Folder;
import com.docflow.folder.repository.FolderRepository;
import com.docflow.user.entity.User;
import com.docflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link FolderService} 的預設實作，負責資料夾維護與樹狀結構組裝。
 */
@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    /**
     * 建立資料夾並記錄活動。
     *
     * @param request 建立資料
     * @return 建立後的資料夾資訊
     */
    @Override
    @Transactional
    public FolderResponse create(CreateFolderRequest request) {
        User currentUser = getCurrentUser();
        Folder parent = resolveParent(request.getParentId());

        Folder folder = Folder.builder()
                .name(request.getName())
                .parent(parent)
                .sortOrder(request.getSortOrder())
                .createdBy(currentUser)
                .deletedFlag(false)
                .build();

        Folder saved = folderRepository.save(folder);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("name", saved.getName());
        detail.put("parentId", saved.getParent() != null ? saved.getParent().getId() : null);
        activityLogService.log(currentUser.getId(), "FOLDER", saved.getId(), "CREATE", detail);
        return toResponse(saved);
    }

    /**
     * 取得未刪除資料夾的樹狀結構。
     *
     * @return 根節點起始的資料夾樹
     */
    @Override
    @Transactional(readOnly = true)
    public List<FolderTreeResponse> getTree() {
        List<Folder> folders = folderRepository.findAllByDeletedFlagFalseOrderBySortOrderAscIdAsc();
        Map<Long, List<Folder>> childrenMap = folders.stream()
                .filter(folder -> folder.getParent() != null)
                .collect(Collectors.groupingBy(folder -> folder.getParent().getId()));

        return folders.stream()
                .filter(folder -> folder.getParent() == null)
                .sorted(Comparator.comparing(Folder::getSortOrder).thenComparing(Folder::getId))
                .map(folder -> toTreeResponse(folder, childrenMap))
                .toList();
    }

    /**
     * 更新指定資料夾並記錄活動。
     *
     * @param id 資料夾編號
     * @param request 更新資料
     * @return 更新後的資料夾資訊
     */
    @Override
    @Transactional
    public FolderResponse update(Long id, UpdateFolderRequest request) {
        Folder folder = folderRepository.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new BadRequestException("Folder not found"));

        Folder parent = resolveParent(request.getParentId());
        if (parent != null && parent.getId().equals(folder.getId())) {
            throw new BadRequestException("Folder cannot be its own parent");
        }

        folder.setName(request.getName());
        folder.setParent(parent);
        folder.setSortOrder(request.getSortOrder());

        Folder saved = folderRepository.save(folder);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("name", saved.getName());
        detail.put("parentId", saved.getParent() != null ? saved.getParent().getId() : null);
        detail.put("sortOrder", saved.getSortOrder());
        activityLogService.log(getCurrentUser().getId(), "FOLDER", saved.getId(), "UPDATE", detail);
        return toResponse(saved);
    }

    /**
     * 軟刪除指定資料夾；若仍有未刪除子資料夾則拒絕刪除。
     *
     * @param id 資料夾編號
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Folder folder = folderRepository.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new BadRequestException("Folder not found"));

        if (folderRepository.existsByParentIdAndDeletedFlagFalse(id)) {
            throw new BadRequestException("Cannot delete folder with active child folders");
        }

        folder.setDeletedFlag(true);
        folderRepository.save(folder);
        activityLogService.log(getCurrentUser().getId(), "FOLDER", folder.getId(), "DELETE", java.util.Map.of(
                "name", folder.getName()
        ));
    }

    /**
     * 取得目前登入使用者。
     *
     * @return 目前登入使用者
     */
    private User getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Current user not found"));
    }

    /**
     * 解析父資料夾；未提供父節點時回傳 {@code null}。
     *
     * @param parentId 父資料夾編號
     * @return 父資料夾實體或 {@code null}
     */
    private Folder resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return folderRepository.findByIdAndDeletedFlagFalse(parentId)
                .orElseThrow(() -> new BadRequestException("Parent folder not found"));
    }

    /**
     * 將資料夾實體轉為回應物件。
     *
     * @param folder 資料夾實體
     * @return 資料夾回應資料
     */
    private FolderResponse toResponse(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .sortOrder(folder.getSortOrder())
                .createdBy(folder.getCreatedBy().getId())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    /**
     * 依父子關係遞迴組裝樹狀節點。
     *
     * @param folder 目前節點
     * @param childrenMap 依父資料夾編號分組的子節點映射
     * @return 樹狀節點資料
     */
    private FolderTreeResponse toTreeResponse(Folder folder, Map<Long, List<Folder>> childrenMap) {
        List<Folder> children = childrenMap.getOrDefault(folder.getId(), new ArrayList<>());
        children = children.stream()
                .sorted(Comparator.comparing(Folder::getSortOrder).thenComparing(Folder::getId))
                .toList();

        return FolderTreeResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .sortOrder(folder.getSortOrder())
                .createdBy(folder.getCreatedBy().getId())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .children(children.stream().map(child -> toTreeResponse(child, childrenMap)).toList())
                .build();
    }
}
