package com.docflow.folder;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.exception.ForbiddenException;
import com.docflow.common.security.DocflowUserPrincipal;
import com.docflow.folder.dto.CreateFolderRequest;
import com.docflow.folder.dto.FolderResponse;
import com.docflow.folder.dto.FolderTreeResponse;
import com.docflow.folder.dto.ReorderFoldersRequest;
import com.docflow.folder.dto.UpdateFolderRequest;
import com.docflow.folder.entity.Folder;
import com.docflow.folder.repository.FolderRepository;
import com.docflow.folder.service.FolderServiceImpl;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FolderServiceImplTest {

    private FolderRepository folderRepository;
    private UserRepository userRepository;
    private ActivityLogService activityLogService;

    private FolderServiceImpl folderService;

    @BeforeEach
    void setUp() {
        folderRepository = Mockito.mock(FolderRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        activityLogService = Mockito.mock(ActivityLogService.class);
        folderService = new FolderServiceImpl(folderRepository, userRepository, activityLogService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldAssignNextSortOrderForRootFolder() {
        setCurrentUser(1L, UserRole.USER);
        CreateFolderRequest request = new CreateFolderRequest();
        request.setName("Root");
        request.setParentId(null);
        Mockito.when(folderRepository.findTopByDeletedFlagFalseAndParentIsNullOrderBySortOrderDescIdDesc())
                .thenReturn(Optional.of(buildFolder(10L, "Existing", null, 3, 1L)));
        Mockito.when(folderRepository.save(Mockito.any(Folder.class)))
                .thenAnswer(invocation -> {
                    Folder folder = invocation.getArgument(0);
                    folder.setId(11L);
                    return folder;
                });

        FolderResponse response = folderService.create(request);

        assertThat(response.getSortOrder()).isEqualTo(4);
    }

    @Test
    void createShouldAllowFirstRootFolderWhenNoFoldersExist() {
        setCurrentUser(1L, UserRole.USER);
        CreateFolderRequest request = new CreateFolderRequest();
        request.setName("Root");
        request.setParentId(null);
        Mockito.when(folderRepository.findTopByDeletedFlagFalseAndParentIsNullOrderBySortOrderDescIdDesc())
                .thenReturn(Optional.empty());
        Mockito.when(folderRepository.save(Mockito.any(Folder.class)))
                .thenAnswer(invocation -> {
                    Folder folder = invocation.getArgument(0);
                    folder.setId(11L);
                    return folder;
                });

        FolderResponse response = folderService.create(request);

        assertThat(response.getSortOrder()).isEqualTo(0);
        assertThat(response.getParentId()).isNull();
    }

    @Test
    void createShouldAssignNextSortOrderForChildFolder() {
        setCurrentUser(1L, UserRole.USER);
        Folder parent = buildFolder(5L, "Parent", null, 0, 1L);
        CreateFolderRequest request = new CreateFolderRequest();
        request.setName("Child");
        request.setParentId(5L);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(5L))
                .thenReturn(Optional.of(parent));
        Mockito.when(folderRepository.findTopByDeletedFlagFalseAndParentIdOrderBySortOrderDescIdDesc(5L))
                .thenReturn(Optional.of(buildFolder(8L, "Older Child", parent, 6, 1L)));
        Mockito.when(folderRepository.save(Mockito.any(Folder.class)))
                .thenAnswer(invocation -> {
                    Folder folder = invocation.getArgument(0);
                    folder.setId(12L);
                    return folder;
                });

        FolderResponse response = folderService.create(request);

        assertThat(response.getSortOrder()).isEqualTo(7);
        assertThat(response.getParentId()).isEqualTo(5L);
    }

    @Test
    void createShouldRejectWhenUserCreatesFolderUnderAnotherUsersParent() {
        setCurrentUser(1L, UserRole.USER);
        Folder foreignParent = buildFolder(5L, "Parent", null, 0, 2L);
        CreateFolderRequest request = new CreateFolderRequest();
        request.setName("Child");
        request.setParentId(5L);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(5L))
                .thenReturn(Optional.of(foreignParent));

        assertThatThrownBy(() -> folderService.create(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("無權限操作此資料夾");
        Mockito.verify(folderRepository, Mockito.never()).save(Mockito.any(Folder.class));
    }

    @Test
    void createShouldAllowAdminToCreateFolderUnderAnotherUsersParent() {
        setCurrentUser(1L, UserRole.ADMIN);
        Folder foreignParent = buildFolder(5L, "Parent", null, 0, 2L);
        CreateFolderRequest request = new CreateFolderRequest();
        request.setName("Child");
        request.setParentId(5L);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(5L))
                .thenReturn(Optional.of(foreignParent));
        Mockito.when(folderRepository.findTopByDeletedFlagFalseAndParentIdOrderBySortOrderDescIdDesc(5L))
                .thenReturn(Optional.empty());
        Mockito.when(folderRepository.save(Mockito.any(Folder.class)))
                .thenAnswer(invocation -> {
                    Folder folder = invocation.getArgument(0);
                    folder.setId(12L);
                    return folder;
                });

        FolderResponse response = folderService.create(request);

        assertThat(response.getParentId()).isEqualTo(5L);
        assertThat(response.getSortOrder()).isEqualTo(0);
    }

    @Test
    void createShouldAllowManagerToCreateFolderUnderAnotherUsersParent() {
        setCurrentUser(1L, UserRole.MANAGER);
        Folder foreignParent = buildFolder(5L, "Parent", null, 0, 2L);
        CreateFolderRequest request = new CreateFolderRequest();
        request.setName("Child");
        request.setParentId(5L);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(5L))
                .thenReturn(Optional.of(foreignParent));
        Mockito.when(folderRepository.findTopByDeletedFlagFalseAndParentIdOrderBySortOrderDescIdDesc(5L))
                .thenReturn(Optional.empty());
        Mockito.when(folderRepository.save(Mockito.any(Folder.class)))
                .thenAnswer(invocation -> {
                    Folder folder = invocation.getArgument(0);
                    folder.setId(13L);
                    return folder;
                });

        FolderResponse response = folderService.create(request);

        assertThat(response.getParentId()).isEqualTo(5L);
        assertThat(response.getSortOrder()).isEqualTo(0);
    }

    @Test
    void getTreeShouldOnlyReturnCurrentUsersFoldersForUser() {
        setCurrentUser(1L, UserRole.USER);
        Folder mineRoot = buildFolder(1L, "Mine Root", null, 0, 1L);
        Folder mineChild = buildFolder(3L, "Mine Child", mineRoot, 0, 1L);
        Mockito.when(folderRepository.findAllByDeletedFlagFalseAndCreatedByIdOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(mineRoot, mineChild));

        List<FolderTreeResponse> tree = folderService.getTree();

        assertThat(tree).extracting(FolderTreeResponse::getId).containsExactly(1L);
        assertThat(tree.get(0).getChildren()).extracting(FolderTreeResponse::getId).containsExactly(3L);
        Mockito.verify(folderRepository).findAllByDeletedFlagFalseAndCreatedByIdOrderBySortOrderAscIdAsc(1L);
        Mockito.verify(folderRepository, Mockito.never()).findAllByDeletedFlagFalseOrderBySortOrderAscIdAsc();
    }

    @Test
    void getTreeShouldPromoteOrphanUserFolderToRootWhenParentBelongsToAnotherUser() {
        setCurrentUser(1L, UserRole.USER);
        Folder foreignParent = buildFolder(2L, "Foreign Parent", null, 0, 2L);
        Folder mineRoot = buildFolder(1L, "Mine Root", null, 0, 1L);
        Folder mineOrphanChild = buildFolder(3L, "Mine Orphan Child", foreignParent, 0, 1L);
        Mockito.when(folderRepository.findAllByDeletedFlagFalseAndCreatedByIdOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(mineRoot, mineOrphanChild));

        List<FolderTreeResponse> tree = folderService.getTree();

        assertThat(tree).extracting(FolderTreeResponse::getId).containsExactly(1L, 3L);
        assertThat(tree.get(0).getChildren()).isEmpty();
        assertThat(tree.get(1).getChildren()).isEmpty();
    }

    @Test
    void getTreeShouldReturnFullTreeForAdminAndManager() {
        Folder mineRoot = buildFolder(1L, "Mine Root", null, 0, 1L);
        Folder mineChild = buildFolder(3L, "Mine Child", mineRoot, 0, 1L);
        Folder otherRoot = buildFolder(2L, "Other Root", null, 1, 2L);
        Folder otherChild = buildFolder(4L, "Other Child", otherRoot, 0, 2L);
        Mockito.when(folderRepository.findAllByDeletedFlagFalseOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(mineRoot, mineChild, otherRoot, otherChild));

        setCurrentUser(1L, UserRole.ADMIN);
        assertFullTree(folderService.getTree());

        setCurrentUser(1L, UserRole.MANAGER);
        assertFullTree(folderService.getTree());
        Mockito.verify(folderRepository, Mockito.times(2)).findAllByDeletedFlagFalseOrderBySortOrderAscIdAsc();
        Mockito.verify(folderRepository, Mockito.never()).findAllByDeletedFlagFalseAndCreatedByIdOrderBySortOrderAscIdAsc(Mockito.anyLong());
    }

    @Test
    void reorderShouldRewriteSiblingSortOrder() {
        setCurrentUser(1L, UserRole.USER);
        Folder first = buildFolder(1L, "A", null, 0, 1L);
        Folder second = buildFolder(2L, "B", null, 1, 1L);
        Folder third = buildFolder(3L, "C", null, 2, 1L);
        ReorderFoldersRequest request = new ReorderFoldersRequest();
        request.setParentId(null);
        request.setOrderedFolderIds(List.of(3L, 1L, 2L));
        Mockito.when(folderRepository.findAllByDeletedFlagFalseAndParentIsNullOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(first, second, third));

        folderService.reorder(request);

        ArgumentCaptor<Iterable<Folder>> captor = ArgumentCaptor.forClass(Iterable.class);
        Mockito.verify(folderRepository).saveAll(captor.capture());
        List<Folder> saved = (List<Folder>) captor.getValue();
        assertThat(saved).extracting(Folder::getId, Folder::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(3L, 0),
                        org.assertj.core.groups.Tuple.tuple(1L, 1),
                        org.assertj.core.groups.Tuple.tuple(2L, 2)
                );
    }

    @Test
    void reorderShouldRejectWhenOrderedIdsDoNotMatchSiblingSet() {
        setCurrentUser(1L, UserRole.USER);
        Folder first = buildFolder(1L, "A", null, 0, 1L);
        Folder second = buildFolder(2L, "B", null, 1, 1L);
        ReorderFoldersRequest request = new ReorderFoldersRequest();
        request.setParentId(null);
        request.setOrderedFolderIds(List.of(2L));
        Mockito.when(folderRepository.findAllByDeletedFlagFalseAndParentIsNullOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> folderService.reorder(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorderShouldRejectWhenUserCannotModifyAllSiblings() {
        setCurrentUser(1L, UserRole.USER);
        Folder first = buildFolder(1L, "Mine", null, 0, 1L);
        Folder second = buildFolder(2L, "Others", null, 1, 2L);
        ReorderFoldersRequest request = new ReorderFoldersRequest();
        request.setParentId(null);
        request.setOrderedFolderIds(List.of(2L, 1L));
        Mockito.when(folderRepository.findAllByDeletedFlagFalseAndParentIsNullOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> folderService.reorder(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("無權限操作此資料夾");
    }

    @Test
    void updateShouldAssignNextSortOrderWhenParentChanges() {
        setCurrentUser(1L, UserRole.USER);
        Folder currentParent = buildFolder(5L, "Current Parent", null, 0, 1L);
        Folder nextParent = buildFolder(6L, "Next Parent", null, 1, 1L);
        Folder folder = buildFolder(1L, "Child", currentParent, 2, 1L);
        UpdateFolderRequest request = new UpdateFolderRequest();
        request.setName("Child");
        request.setParentId(6L);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(folder));
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(6L))
                .thenReturn(Optional.of(nextParent));
        Mockito.when(folderRepository.findTopByDeletedFlagFalseAndParentIdOrderBySortOrderDescIdDesc(6L))
                .thenReturn(Optional.of(buildFolder(7L, "Existing", nextParent, 4, 1L)));
        Mockito.when(folderRepository.save(Mockito.any(Folder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FolderResponse response = folderService.update(1L, request);

        assertThat(response.getParentId()).isEqualTo(6L);
        assertThat(response.getSortOrder()).isEqualTo(5);
    }

    @Test
    void updateShouldAllowOrphanFolderRenameWithoutChangingForeignParent() {
        setCurrentUser(1L, UserRole.USER);
        Folder foreignParent = buildFolder(6L, "Foreign Parent", null, 0, 2L);
        Folder folder = buildFolder(1L, "Orphan", foreignParent, 2, 1L);
        UpdateFolderRequest request = new UpdateFolderRequest();
        request.setName("Renamed");
        request.setParentId(6L);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(folder));
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(6L))
                .thenReturn(Optional.of(foreignParent));
        Mockito.when(folderRepository.save(Mockito.any(Folder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FolderResponse response = folderService.update(1L, request);

        assertThat(response.getName()).isEqualTo("Renamed");
        assertThat(response.getParentId()).isEqualTo(6L);
    }

    @Test
    void updateShouldRejectWhenUserMovesOwnFolderUnderAnotherUsersParent() {
        setCurrentUser(1L, UserRole.USER);
        Folder currentParent = buildFolder(5L, "Current Parent", null, 0, 1L);
        Folder nextParent = buildFolder(6L, "Next Parent", null, 1, 2L);
        Folder folder = buildFolder(1L, "Child", currentParent, 2, 1L);
        UpdateFolderRequest request = new UpdateFolderRequest();
        request.setName("Child");
        request.setParentId(6L);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(folder));
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(6L))
                .thenReturn(Optional.of(nextParent));

        assertThatThrownBy(() -> folderService.update(1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("無權限操作此資料夾");
        Mockito.verify(folderRepository, Mockito.never()).save(Mockito.any(Folder.class));
    }

    @Test
    void updateShouldRejectWhenUserEditsAnotherUsersFolder() {
        setCurrentUser(1L, UserRole.USER);
        Folder folder = buildFolder(1L, "Foreign", null, 0, 2L);
        UpdateFolderRequest request = new UpdateFolderRequest();
        request.setName("Updated");
        request.setParentId(null);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(folder));

        assertThatThrownBy(() -> folderService.update(1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("無權限操作此資料夾");
        Mockito.verify(folderRepository, Mockito.never()).save(Mockito.any(Folder.class));
    }

    @Test
    void updateShouldAllowManagerToEditAnotherUsersFolder() {
        setCurrentUser(1L, UserRole.MANAGER);
        Folder folder = buildFolder(1L, "Foreign", null, 0, 2L);
        UpdateFolderRequest request = new UpdateFolderRequest();
        request.setName("Updated");
        request.setParentId(null);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(folder));
        Mockito.when(folderRepository.save(Mockito.any(Folder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FolderResponse response = folderService.update(1L, request);

        assertThat(response.getName()).isEqualTo("Updated");
    }

    @Test
    void deleteShouldRejectWhenUserDeletesAnotherUsersFolder() {
        setCurrentUser(1L, UserRole.USER);
        Folder folder = buildFolder(1L, "Foreign", null, 0, 2L);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(folder));

        assertThatThrownBy(() -> folderService.delete(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("無權限操作此資料夾");
        Mockito.verify(folderRepository, Mockito.never()).save(Mockito.any(Folder.class));
    }

    @Test
    void deleteShouldAllowManagerToDeleteAnotherUsersFolder() {
        setCurrentUser(1L, UserRole.MANAGER);
        Folder folder = buildFolder(1L, "Foreign", null, 0, 2L);
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(folder));
        Mockito.when(folderRepository.existsByParentIdAndDeletedFlagFalse(1L))
                .thenReturn(false);
        Mockito.when(folderRepository.save(Mockito.any(Folder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        folderService.delete(1L);

        Mockito.verify(folderRepository).save(Mockito.any(Folder.class));
    }

    private void setCurrentUser(Long userId, UserRole role) {
        User user = buildUser(userId, role);
        DocflowUserPrincipal principal = new DocflowUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        Mockito.lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private Folder buildFolder(Long id, String name, Folder parent, int sortOrder, Long createdBy) {
        return Folder.builder()
                .id(id)
                .name(name)
                .parent(parent)
                .sortOrder(sortOrder)
                .createdBy(buildUser(createdBy, UserRole.USER))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedFlag(false)
                .build();
    }

    private void assertFullTree(List<FolderTreeResponse> tree) {
        assertThat(tree).extracting(FolderTreeResponse::getId).containsExactly(1L, 2L);
        assertThat(tree.get(0).getChildren()).extracting(FolderTreeResponse::getId).containsExactly(3L);
        assertThat(tree.get(1).getChildren()).extracting(FolderTreeResponse::getId).containsExactly(4L);
    }

    private User buildUser(Long id, UserRole role) {
        return User.builder()
                .id(id)
                .username("user" + id)
                .email("user" + id + "@example.com")
                .passwordHash("hash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
