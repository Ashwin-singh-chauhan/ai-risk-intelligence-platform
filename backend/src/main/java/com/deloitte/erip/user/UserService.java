package com.deloitte.erip.user;

import com.deloitte.erip.audit.AuditService;
import com.deloitte.erip.common.exception.BadRequestException;
import com.deloitte.erip.common.exception.ResourceNotFoundException;
import com.deloitte.erip.user.dto.CreateUserRequest;
import com.deloitte.erip.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("A user with this email already exists");
        }
        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .businessUnit(request.businessUnit())
                .active(true)
                .build();
        User saved = userRepository.save(user);
        auditService.record("USER_CREATED", "USER", saved.getId().toString(), Map.of("role", saved.getRole().name()));
        return UserResponse.from(saved);
    }

    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    public UserResponse getUser(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    @Transactional
    public UserResponse setActive(UUID id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
        user.setActive(active);
        User saved = userRepository.save(user);
        auditService.record(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", id.toString(), null);
        return UserResponse.from(saved);
    }
}
