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
        log.info("Updating folder: folderId={}, name={}, parentId={}", id, request.getName(), request.getParentId());
        Folder folder = folderRepository.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new BadRequestException("Folder not found"));

        Folder parent = resolveParent(request.getParentId());
        if (parent != null && parent.getId().equals(folder.getId())) {
            throw new BadRequestException("Folder cannot be its own parent");
        }
        boolean parentChanged = !sameParent(folder.getParent(), parent);

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

    /**
     * 重新排序指定父資料夾下的所有子資料夾。
     *
     * @param request 包含新排序順序的資訊
     * @throws BadRequestException 若排序資訊不符合現有子資料夾集合
     * @throws ForbiddenException 若目前使用者無權限操作該資料夾
     */
    @Override
    @Transactional
    public void reorder(ReorderFoldersRequest request) {
        log.info("Reordering folders: parentId={}, folderCount={}", request.getParentId(), request.getOrderedFolderIds().size());
        User currentUser = getCurrentUser();
        List<Folder> siblings = loadSiblings(request.getParentId());
        validateReorderRequest(request.getOrderedFolderIds(), siblings);
        assertCanReorderSiblings(currentUser, siblings);

        Map<Long, Folder> folderMap = siblings.stream()
                .collect(Collectors.toMap(Folder::getId, folder -> folder));
        List<Folder> reordered = new ArrayList<>();
        for (int index = 0; index < request.getOrderedFolderIds().size(); index++) {
            Folder folder = folderMap.get(request.getOrderedFolderIds().get(index));
            folder.setSortOrder(index);
            reordered.add(folder);
            log.debug("Updated sort order: folderId={}, sortOrder={}", folder.getId(), index);
        }
        folderRepository.saveAll(reordered);
        log.info("Folders reordered successfully: parentId={}, reorderedCount={}", request.getParentId(), reordered.size());
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

    /**
     * 計算指定父資料夾下一個新建資料夾的排序序號。
     * 若無既有資料夾則傳回 0，否則傳回最後一個資料夾的序號加 1。
     *
     * @param parent 父資料夾（可為 {@code null} 表示根層級）
     * @return 下一個排序序號
     */
    private int nextSortOrder(Folder parent) {
        log.debug("Computing next sort order for parent: parentId={}", parent != null ? parent.getId() : null);
        Optional<Folder> lastFolder = parent == null
                ? folderRepository.findTopByDeletedFlagFalseAndParentIsNullOrderBySortOrderDescIdDesc()
                : folderRepository.findTopByDeletedFlagFalseAndParentIdOrderBySortOrderDescIdDesc(parent.getId());
        int nextOrder = lastFolder.map(folder -> folder.getSortOrder() + 1).orElse(0);
        log.debug("Computed next sort order: nextSortOrder={}", nextOrder);
        return nextOrder;
    }

    /**
     * 判斷兩個父資料夾是否相同。
     * 同時為 {@code null} 視為相同；其他情況需比較編號。
     *
     * @param currentParent 目前的父資料夾
     * @param nextParent 欲變更的父資料夾
     * @return 若兩者相同則傳回 {@code true}，否則傳回 {@code false}
     */
    private boolean sameParent(Folder currentParent, Folder nextParent) {
        log.debug("Comparing parent folders: currentParentId={}, nextParentId={}",
                currentParent != null ? currentParent.getId() : null,
                nextParent != null ? nextParent.getId() : null);
        if (currentParent == null && nextParent == null) {
            return true;
        }
        if (currentParent == null || nextParent == null) {
            return false;
        }
        boolean isSame = currentParent.getId() != null && currentParent.getId().equals(nextParent.getId());
        log.debug("Parent comparison result: isSame={}", isSame);
        return isSame;
    }

    /**
     * 載入指定父資料夾下的所有子資料夾（未刪除），並依排序序號與編號排序。
     *
     * @param parentId 父資料夾編號，{@code null} 表示載入根層級資料夾
     * @return 子資料夾清單
     */
    private List<Folder> loadSiblings(Long parentId) {
        log.debug("Loading sibling folders: parentId={}", parentId);
        List<Folder> siblings = parentId == null
                ? folderRepository.findAllByDeletedFlagFalseAndParentIsNullOrderBySortOrderAscIdAsc()
                : folderRepository.findAllByDeletedFlagFalseAndParentIdOrderBySortOrderAscIdAsc(parentId);
        log.debug("Loaded sibling folders: count={}", siblings.size());
        return siblings;
    }

    /**
     * 驗證排序請求的合法性。
     * 確保請求中的資料夾編號集合與實際子資料夾集合相符，數量與內容均須一致。
     *
     * @param orderedFolderIds 請求中的排序後資料夾編號清單
     * @param siblings 實際的子資料夾清單
     * @throws BadRequestException 若編號數量或內容不符
     */
    private void validateReorderRequest(List<Long> orderedFolderIds, List<Folder> siblings) {
        log.debug("Validating reorder request: requestedCount={}, actualCount={}", orderedFolderIds.size(), siblings.size());
        if (orderedFolderIds.size() != siblings.size()) {
            log.warn("Reorder request size mismatch: requested={}, actual={}", orderedFolderIds.size(), siblings.size());
            throw new BadRequestException("Reorder request must include the full sibling set");
        }
        List<Long> siblingIds = siblings.stream().map(Folder::getId).sorted().toList();
        List<Long> requestedIds = orderedFolderIds.stream().sorted().toList();
        if (!siblingIds.equals(requestedIds)) {
            log.warn("Reorder request folder IDs mismatch");
            throw new BadRequestException("Reorder request does not match current sibling set");
        }
        log.debug("Reorder request validation passed");
    }

    /**
     * 驗證目前使用者是否有權限重新排序指定的子資料夾集合。
     * 系統管理員和主管可重排所有資料夾；其他使用者僅能重排自己建立的資料夾。
     *
     * @param currentUser 目前登入的使用者
     * @param siblings 欲重新排序的子資料夾清單
     * @throws ForbiddenException 若使用者無權限操作任一子資料夾
     */
    private void assertCanReorderSiblings(User currentUser, List<Folder> siblings) {
        log.debug("Checking reorder permission for user: userId={}, role={}, siblingCount={}", 
                currentUser.getId(), currentUser.getRole(), siblings.size());
        if (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.MANAGER) {
            log.debug("User has privileged role, allowing reorder");
            return;
        }
        boolean canModifyAll = siblings.stream()
                .allMatch(folder -> folder.getCreatedBy() != null
                        && folder.getCreatedBy().getId() != null
                        && folder.getCreatedBy().getId().equals(currentUser.getId()));
        if (!canModifyAll) {
            log.warn("User lacks permission to reorder siblings: userId={}, reason=not_creator_of_all", currentUser.getId());
            throw new ForbiddenException("無權限操作此資料夾");
        }
        log.debug("Reorder permission check passed for user: userId={}", currentUser.getId());
    }

    /**
     * 將資料夾實體轉為回應物件。
     *
     * @param folder 資料夾實體
     * @return 資料夾回應資料
     */
    private FolderResponse toResponse(Folder folder) {
        log.trace("Converting folder entity to response: folderId={}, name={}", folder.getId(), folder.getName());
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
        log.trace("Building tree response for folder: folderId={}, name={}", folder.getId(), folder.getName());
        List<Folder> children = childrenMap.getOrDefault(folder.getId(), new ArrayList<>());
        children = children.stream()
                .sorted(Comparator.comparing(Folder::getSortOrder).thenComparing(Folder::getId))
                .toList();

        log.trace("Folder tree node prepared: folderId={}, childrenCount={}", folder.getId(), children.size());
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
