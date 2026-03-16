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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

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
        activityLogService.log(currentUser.getId(), "FOLDER", saved.getId(), "CREATE", java.util.Map.of(
                "name", saved.getName(),
                "parentId", saved.getParent() != null ? saved.getParent().getId() : null
        ));
        return toResponse(saved);
    }

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
        activityLogService.log(getCurrentUser().getId(), "FOLDER", saved.getId(), "UPDATE", java.util.Map.of(
                "name", saved.getName(),
                "parentId", saved.getParent() != null ? saved.getParent().getId() : null,
                "sortOrder", saved.getSortOrder()
        ));
        return toResponse(saved);
    }

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

    private User getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Current user not found"));
    }

    private Folder resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return folderRepository.findByIdAndDeletedFlagFalse(parentId)
                .orElseThrow(() -> new BadRequestException("Parent folder not found"));
    }

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
