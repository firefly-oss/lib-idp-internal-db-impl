package com.firefly.idp.internaldb.service;

import com.firefly.idp.internaldb.domain.Role;
import com.firefly.idp.internaldb.domain.UserRole;
import com.firefly.idp.internaldb.repository.RoleRepository;
import com.firefly.idp.internaldb.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private RoleService roleService;

    private Role testRole;
    private UUID testRoleId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testRoleId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        
        testRole = Role.builder()
                .id(testRoleId)
                .name("ADMIN")
                .description("Administrator role")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testRole.markAsNotNew();
    }

    @Test
    void shouldCreateRole() {
        // Given
        String roleName = "USER";
        String description = "User role";

        when(roleRepository.existsByName(roleName)).thenReturn(Mono.just(false));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(UUID.randomUUID());
            return Mono.just(role);
        });

        // When & Then
        StepVerifier.create(roleService.createRole(roleName, description))
                .assertNext(role -> {
                    assertThat(role.getName()).isEqualTo(roleName);
                    assertThat(role.getDescription()).isEqualTo(description);
                    assertThat(role.getId()).isNotNull();
                })
                .verifyComplete();

        verify(roleRepository).existsByName(roleName);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void shouldFailToCreateDuplicateRole() {
        // Given
        String roleName = "ADMIN";

        when(roleRepository.existsByName(roleName)).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(roleService.createRole(roleName, null))
                .expectErrorMessage("Role already exists: ADMIN")
                .verify();

        verify(roleRepository).existsByName(roleName);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void shouldCreateMultipleRoles() {
        // Given
        when(roleRepository.existsByName(anyString())).thenReturn(Mono.just(false));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(UUID.randomUUID());
            return Mono.just(role);
        });

        // When & Then
        StepVerifier.create(roleService.createRoles(Arrays.asList("USER", "ADMIN", "GUEST")))
                .expectNextCount(3)
                .verifyComplete();

        verify(roleRepository, times(3)).existsByName(anyString());
        verify(roleRepository, times(3)).save(any(Role.class));
    }

    @Test
    void shouldGetRoleByName() {
        // Given
        when(roleRepository.findByName("ADMIN")).thenReturn(Mono.just(testRole));

        // When & Then
        StepVerifier.create(roleService.getRoleByName("ADMIN"))
                .assertNext(role -> {
                    assertThat(role.getName()).isEqualTo("ADMIN");
                    assertThat(role.getDescription()).isEqualTo("Administrator role");
                })
                .verifyComplete();

        verify(roleRepository).findByName("ADMIN");
    }

    @Test
    void shouldGetAllRoles() {
        // Given
        Role role1 = Role.builder().id(UUID.randomUUID()).name("USER").build();
        Role role2 = Role.builder().id(UUID.randomUUID()).name("ADMIN").build();

        when(roleRepository.findAll()).thenReturn(Flux.just(role1, role2));

        // When & Then
        StepVerifier.create(roleService.getAllRoles())
                .expectNext(role1, role2)
                .verifyComplete();

        verify(roleRepository).findAll();
    }

    @Test
    void shouldGetUserRoles() {
        // Given
        UUID roleId1 = UUID.randomUUID();
        UUID roleId2 = UUID.randomUUID();

        UserRole userRole1 = UserRole.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .roleId(roleId1)
                .build();

        UserRole userRole2 = UserRole.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .roleId(roleId2)
                .build();

        Role role1 = Role.builder().id(roleId1).name("USER").build();
        Role role2 = Role.builder().id(roleId2).name("ADMIN").build();

        when(userRoleRepository.findByUserId(testUserId)).thenReturn(Flux.just(userRole1, userRole2));
        when(roleRepository.findById(roleId1)).thenReturn(Mono.just(role1));
        when(roleRepository.findById(roleId2)).thenReturn(Mono.just(role2));

        // When & Then
        StepVerifier.create(roleService.getUserRoles(testUserId))
                .expectNext("USER", "ADMIN")
                .verifyComplete();

        verify(userRoleRepository).findByUserId(testUserId);
        verify(roleRepository).findById(roleId1);
        verify(roleRepository).findById(roleId2);
    }

    @Test
    void shouldAssignRoleToUser() {
        // Given
        String roleName = "ADMIN";

        when(roleRepository.findByName(roleName)).thenReturn(Mono.just(testRole));
        when(userRoleRepository.findByUserIdAndRoleId(testUserId, testRoleId)).thenReturn(Mono.empty());
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When & Then
        StepVerifier.create(roleService.assignRoleToUser(testUserId, roleName))
                .verifyComplete();

        verify(roleRepository).findByName(roleName);
        verify(userRoleRepository).findByUserIdAndRoleId(testUserId, testRoleId);
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void shouldNotAssignRoleTwice() {
        // Given
        String roleName = "ADMIN";
        UserRole existingUserRole = UserRole.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .roleId(testRoleId)
                .build();

        when(roleRepository.findByName(roleName)).thenReturn(Mono.just(testRole));
        when(userRoleRepository.findByUserIdAndRoleId(testUserId, testRoleId)).thenReturn(Mono.just(existingUserRole));

        // When & Then
        StepVerifier.create(roleService.assignRoleToUser(testUserId, roleName))
                .verifyComplete();

        verify(roleRepository).findByName(roleName);
        verify(userRoleRepository).findByUserIdAndRoleId(testUserId, testRoleId);
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void shouldAssignMultipleRolesToUser() {
        // Given
        Role role1 = Role.builder().id(UUID.randomUUID()).name("USER").build();
        Role role2 = Role.builder().id(UUID.randomUUID()).name("ADMIN").build();

        when(roleRepository.findByName("USER")).thenReturn(Mono.just(role1));
        when(roleRepository.findByName("ADMIN")).thenReturn(Mono.just(role2));
        when(userRoleRepository.findByUserIdAndRoleId(any(), any())).thenReturn(Mono.empty());
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When & Then
        StepVerifier.create(roleService.assignRolesToUser(testUserId, Arrays.asList("USER", "ADMIN")))
                .verifyComplete();

        verify(roleRepository).findByName("USER");
        verify(roleRepository).findByName("ADMIN");
        verify(userRoleRepository, times(2)).save(any(UserRole.class));
    }

    @Test
    void shouldRemoveRoleFromUser() {
        // Given
        String roleName = "ADMIN";

        when(roleRepository.findByName(roleName)).thenReturn(Mono.just(testRole));
        when(userRoleRepository.deleteByUserIdAndRoleId(testUserId, testRoleId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(roleService.removeRoleFromUser(testUserId, roleName))
                .verifyComplete();

        verify(roleRepository).findByName(roleName);
        verify(userRoleRepository).deleteByUserIdAndRoleId(testUserId, testRoleId);
    }

    @Test
    void shouldDeleteRole() {
        // Given
        String roleName = "ADMIN";
        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .roleId(testRoleId)
                .build();

        when(roleRepository.findByName(roleName)).thenReturn(Mono.just(testRole));
        when(userRoleRepository.findByRoleId(testRoleId)).thenReturn(Flux.just(userRole));
        when(userRoleRepository.delete(any(UserRole.class))).thenReturn(Mono.empty());
        when(roleRepository.delete(testRole)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(roleService.deleteRole(roleName))
                .verifyComplete();

        verify(roleRepository).findByName(roleName);
        verify(userRoleRepository).findByRoleId(testRoleId);
        verify(userRoleRepository).delete(userRole);
        verify(roleRepository).delete(testRole);
    }

    @Test
    void shouldFailToDeleteNonExistentRole() {
        // Given
        String roleName = "NONEXISTENT";

        when(roleRepository.findByName(roleName)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(roleService.deleteRole(roleName))
                .expectErrorMessage("Role not found: NONEXISTENT")
                .verify();

        verify(roleRepository).findByName(roleName);
        verify(roleRepository, never()).delete(any(Role.class));
    }
}
