package com.volunnear.admin;

import com.volunnear.activity.ActivityService;
import com.volunnear.activity.dto.ActivityResponseDto;
import com.volunnear.admin.dto.AdminUserPageResponseDto;
import com.volunnear.admin.dto.AdminUserResponseDto;
import com.volunnear.application.ApplicationService;
import com.volunnear.auth.AppUser;
import com.volunnear.auth.AppUserRepository;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.auth.Role;
import com.volunnear.auth.UserBanned;
import com.volunnear.common.exception.ConflictException;
import com.volunnear.common.exception.DataNotFoundException;
import com.volunnear.organization.OrganizationAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Locale;

/**
 * Platform administration (Q21 … Q32). {@code SecurityConfig} already limits {@code /admin/**} to admins;
 * every method checks the role again, as the service layer owns authorization.
 */
@Service
@RequiredArgsConstructor
public class AdminService {
    private final AppUserRepository appUserRepository;
    private final ApplicationService applicationService;
    private final ActivityService activityService;
    private final OrganizationAccess organizationAccess;
    private final ApplicationEventPublisher publisher;

    /** Q29: every filter is optional; {@code query} matches a part of the username or email, ignoring case. */
    @Transactional(readOnly = true)
    public AdminUserPageResponseDto listUsers(String query, Role role, Boolean banned, int page, int size,
                                              CustomUserDetails admin) {
        requireAdmin(admin);
        Specification<AppUser> filter = (root, q, cb) -> cb.conjunction();
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            filter = filter.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)));
        }
        if (role != null) {
            filter = filter.and((root, q, cb) -> cb.isMember(role, root.get("roles")));
        }
        if (banned != null) {
            filter = filter.and((root, q, cb) -> cb.equal(root.get("isBanned"), banned));
        }
        Page<AppUser> result = appUserRepository.findAll(filter, PageRequest.of(page, size, Sort.by("id")));
        return new AdminUserPageResponseDto(result.map(AdminService::toDto).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /**
     * Q23, Q25, Q26, Q30. A repeated ban changes nothing but deletes the sessions again, so a ban whose
     * session cleanup failed can simply be repeated.
     */
    @Transactional
    public AdminUserResponseDto ban(Long userId, CustomUserDetails admin) {
        requireAdmin(admin);
        AppUser user = find(userId);
        if (user.getId().equals(admin.getUser().getId())) {
            throw new ConflictException("You cannot ban yourself");
        }
        if (user.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new ConflictException("An admin cannot be banned");
        }
        if (!user.isBanned()) {
            user.setBanned(true);
            if (user.getRoles().contains(Role.ROLE_VOLUNTEER)) {
                applicationService.releaseBannedVolunteer(user.getId());
            }
            if (user.getRoles().contains(Role.ROLE_ORGANIZATION)) {
                organizationAccess.organizationIdOf(user.getId()).ifPresent(activityService::cancelUpcomingOf);
            }
        }
        publisher.publishEvent(new UserBanned(user.getUsername()));
        return toDto(user);
    }

    /** Q27: only the flag; nothing withdrawn or cancelled comes back. */
    @Transactional
    public AdminUserResponseDto unban(Long userId, CustomUserDetails admin) {
        requireAdmin(admin);
        AppUser user = find(userId);
        user.setBanned(false);
        return toDto(user);
    }

    /** Q31: removing an activity means cancelling it, with the notifications of Q7b. */
    @Transactional
    public ActivityResponseDto cancelActivity(Long activityId, CustomUserDetails admin) {
        requireAdmin(admin);
        return activityService.cancelAsAdmin(activityId);
    }

    private static void requireAdmin(CustomUserDetails caller) {
        if (caller == null || !caller.getUser().getRoles().contains(Role.ROLE_ADMIN)) {
            throw new AccessDeniedException("Only an admin can do this");
        }
    }

    private AppUser find(Long userId) {
        return appUserRepository.findAppUserById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found"));
    }

    private static AdminUserResponseDto toDto(AppUser user) {
        return new AdminUserResponseDto(user.getId(), user.getUsername(), user.getEmail(),
                user.getRoles().isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(user.getRoles()),
                user.isBanned(), user.getCreatedAt());
    }
}
