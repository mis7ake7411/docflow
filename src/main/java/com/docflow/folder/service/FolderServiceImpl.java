package com.docflow.folder.service;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.exception.ForbiddenException;
import com.docflow.common.security.SecurityUtils;
import com.docflow.folder.dto.CreateFolderRequest;
import com.docflow.folder.dto.FolderResponse;
import com.docflow.folder.dto.FolderTreeResponse;
import com.docflow.folder.dto.ReorderFoldersRequest;
import com.docflow.folder.dto.UpdateFolderRequest;
import com.docflow.folder.entity.Folder;
import com.docflow.folder.repository.FolderRepository;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link FolderService} 的預設實作，負責資料夾維護與樹狀結構組裝。
 */
@Service
@Slf4j
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
        log.info("Creating folder: name={}, parentId={}", request.getName(), request.getParentId());
        User currentUser = getCurrentUser();
        Folder parent = resolveParent(request.getParentId());
        assertCanUseParent(currentUser, parent);

        Folder folder = Folder.builder()
                .name(request.getName())
                .parent(parent)
                .sortOrder(nextSortOrder(parent))
                .createdBy(currentUser)
                .deletedFlag(false)
                .build();

        Folder saved = folderRepository.save(folder);
        log.info("Folder created successfully: folderId={}, createdBy={}", saved.getId(), currentUser.getId());
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
        log.debug("Loading folder tree");
        User currentUser = getCurrentUser();
        List<Folder> folders = loadFoldersForTree(currentUser);
        Map<Long, Folder> folderById = folders.stream()
                .filter(folder -> folder.getId() != null)
                .collect(Collectors.toMap(Folder::getId, folder -> folder));
        Map<Long, List<Folder>> childrenMap = folders.stream()
                .filter(folder -> folder.getParent() != null)
                .collect(Collectors.groupingBy(folder -> folder.getParent().getId()));
        List<Folder> rootFolders = folders.stream()
                .filter(folder -> folder.getParent() == null
                        || !folderById.containsKey(folder.getParent().getId()))
                .sorted(Comparator.comparing(Folder::getSortOrder).thenComparing(Folder::getId))
                .toList();

        return rootFolders.stream()
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
        log.info("Updating folder: folderId={}, name={}, parentId={}", id, request.getName(), request.getParentId());
        Folder folder = folderRepository.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new BadRequestException("Folder not found"));
        User currentUser = getCurrentUser();
        assertCanModifyFolder(currentUser, folder);

        Folder parent = resolveParent(request.getParentId());
        if (parent != null && parent.getId().equals(folder.getId())) {
            throw new BadRequestException("Folder cannot be its own parent");
        }
        boolean parentChanged = !sameParent(folder.getParent(), parent);
        if (parentChanged) {
            assertCanUseParent(currentUser, parent);
        }

        folder.setName(request.getName());
        folder.setParent(parent);
        if (parentChanged) {
            folder.setSortOrder(nextSortOrder(parent));
        }

        Folder saved = folderRepository.save(folder);
        log.info("Folder updated successfully: folderId={}", saved.getId());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("name", saved.getName());
        detail.put("parentId", saved.getParent() != null ? saved.getParent().getId() : null);
        activityLogService.log(getCurrentUser().getId(), "FOLDER", saved.getId(), "UPDATE", detail);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void reorder(ReorderFoldersRequest request) {
        User currentUser = getCurrentUser();
        List<Folder> siblings = loadSiblings(currentUser, request.getParentId());
        validateReorderRequest(request.getOrderedFolderIds(), siblings);
        assertCanReorderSiblings(currentUser, siblings);

        Map<Long, Folder> folderMap = siblings.stream()
                .collect(Collectors.toMap(Folder::getId, folder -> folder));
        List<Folder> reordered = new ArrayList<>();
        for (int index = 0; index < request.getOrderedFolderIds().size(); index++) {
            Folder folder = folderMap.get(request.getOrderedFolderIds().get(index));
            folder.setSortOrder(index);
            reordered.add(folder);
        }
        folderRepository.saveAll(reordered);
    }

    /**
     * 軟刪除指定資料夾；若仍有未刪除子資料夾則拒絕刪除。
     *
     * @param id 資料夾編號
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting folder: folderId={}", id);
        Folder folder = folderRepository.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new BadRequestException("Folder not found"));
        User currentUser = getCurrentUser();
        assertCanModifyFolder(currentUser, folder);

        if (folderRepository.existsByParentIdAndDeletedFlagFalse(id)) {
            log.warn("Folder deletion rejected due to active children: folderId={}", id);
            throw new BadRequestException("Cannot delete folder with active child folders");
        }

        folder.setDeletedFlag(true);
        folderRepository.save(folder);
        log.info("Folder soft-deleted successfully: folderId={}", folder.getId());
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
        log.debug("Resolving current user for folder operation: userId={}", userId);
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
        log.debug("Resolving parent folder: parentId={}", parentId);
        return folderRepository.findByIdAndDeletedFlagFalse(parentId)
                .orElseThrow(() -> new BadRequestException("Parent folder not found"));
    }

    private int nextSortOrder(Folder parent) {
        Optional<Folder> lastFolder = parent == null
                ? folderRepository.findTopByDeletedFlagFalseAndParentIsNullOrderBySortOrderDescIdDesc()
                : folderRepository.findTopByDeletedFlagFalseAndParentIdOrderBySortOrderDescIdDesc(parent.getId());
        return lastFolder.map(folder -> folder.getSortOrder() + 1).orElse(0);
    }

    private boolean sameParent(Folder currentParent, Folder nextParent) {
        if (currentParent == null && nextParent == null) {
            return true;
        }
        if (currentParent == null || nextParent == null) {
            return false;
        }
        return currentParent.getId() != null && currentParent.getId().equals(nextParent.getId());
    }

    private boolean canSeeAllFolders(User currentUser) {
        return currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.MANAGER;
    }

    private void assertCanModifyFolder(User currentUser, Folder folder) {
        if (canSeeAllFolders(currentUser)) {
            return;
        }
        if (folder.getCreatedBy() == null
                || folder.getCreatedBy().getId() == null
                || !folder.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("無權限操作此資料夾");
        }
    }

    private void assertCanUseParent(User currentUser, Folder parent) {
        if (parent == null || canSeeAllFolders(currentUser)) {
            return;
        }
        if (parent.getCreatedBy() == null
                || parent.getCreatedBy().getId() == null
                || !parent.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("無權限操作此資料夾");
        }
    }

    private List<Folder> loadFoldersForTree(User currentUser) {
        if (canSeeAllFolders(currentUser)) {
            return folderRepository.findAllByDeletedFlagFalseOrderBySortOrderAscIdAsc();
        }
        return folderRepository.findAllByDeletedFlagFalseAndCreatedByIdOrderBySortOrderAscIdAsc(currentUser.getId());
    }

    private List<Folder> loadSiblings(User currentUser, Long parentId) {
        if (canSeeAllFolders(currentUser)) {
            return parentId == null
                    ? folderRepository.findAllByDeletedFlagFalseAndParentIsNullOrderBySortOrderAscIdAsc()
                    : folderRepository.findAllByDeletedFlagFalseAndParentIdOrderBySortOrderAscIdAsc(parentId);
        }
        List<Folder> userFolders = folderRepository.findAllByDeletedFlagFalseAndCreatedByIdOrderBySortOrderAscIdAsc(currentUser.getId());
        if (parentId == null) {
            Map<Long, Folder> folderById = userFolders.stream()
                    .filter(folder -> folder.getId() != null)
                    .collect(Collectors.toMap(Folder::getId, folder -> folder));
            return userFolders.stream()
                    .filter(folder -> folder.getParent() == null
                            || folder.getParent().getId() == null
                            || !folderById.containsKey(folder.getParent().getId()))
                    .sorted(Comparator.comparing(Folder::getSortOrder).thenComparing(Folder::getId))
                    .toList();
        }
        return folderRepository.findAllByDeletedFlagFalseAndParentIdAndCreatedByIdOrderBySortOrderAscIdAsc(parentId, currentUser.getId());
    }

    private void validateReorderRequest(List<Long> orderedFolderIds, List<Folder> siblings) {
        if (orderedFolderIds.size() != siblings.size()) {
            throw new BadRequestException("Reorder request must include the full sibling set");
        }
        List<Long> siblingIds = siblings.stream().map(Folder::getId).sorted().toList();
        List<Long> requestedIds = orderedFolderIds.stream().sorted().toList();
        if (!siblingIds.equals(requestedIds)) {
            throw new BadRequestException("Reorder request does not match current sibling set");
        }
    }

    private void assertCanReorderSiblings(User currentUser, List<Folder> siblings) {
        if (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.MANAGER) {
            return;
        }
        boolean canModifyAll = siblings.stream()
                .allMatch(folder -> folder.getCreatedBy() != null
                        && folder.getCreatedBy().getId() != null
                        && folder.getCreatedBy().getId().equals(currentUser.getId()));
        if (!canModifyAll) {
            throw new ForbiddenException("無權限操作此資料夾");
        }
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
