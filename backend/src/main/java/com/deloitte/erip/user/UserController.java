package com.deloitte.erip.user;

import com.deloitte.erip.common.dto.PageResponse;
import com.deloitte.erip.user.dto.CreateUserRequest;
import com.deloitte.erip.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Administer platform users and RBAC roles (ADMIN only)")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new platform user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(userService.listUsers(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single user by id")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user account")
    public ResponseEntity<UserResponse> setActive(@PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(userService.setActive(id, active));
    }
}
