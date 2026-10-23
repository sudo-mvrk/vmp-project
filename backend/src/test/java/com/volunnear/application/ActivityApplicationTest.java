package com.volunnear.application;

import com.volunnear.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The transition table of Q5a, with the invitation rules of Q5h and Q5i. */
class ActivityApplicationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 17, 12, 0);

    @Test
    void newApplicationIsPendingWithoutDecision() {
        ActivityApplication application = newApplication();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(application.getAppliedAt()).isEqualTo(NOW);
        assertThat(application.getDecidedAt()).isNull();
    }

    @Test
    void pendingCanBeAcceptedRejectedOrWithdrawn() {
        ActivityApplication accepted = newApplication();
        accepted.accept(NOW.plusHours(1));
        assertThat(accepted.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(accepted.getDecidedAt()).isEqualTo(NOW.plusHours(1));

        ActivityApplication rejected = newApplication();
        rejected.reject(NOW);
        assertThat(rejected.getStatus()).isEqualTo(ApplicationStatus.REJECTED);

        ActivityApplication withdrawn = newApplication();
        withdrawn.withdraw(NOW);
        assertThat(withdrawn.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void acceptedCanBeWithdrawn() {
        ActivityApplication application = withStatus(ApplicationStatus.ACCEPTED);

        application.withdraw(NOW);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void onlyPendingCanBeAcceptedOrRejected(ApplicationStatus status) {
        ActivityApplication application = withStatus(status);

        assertThatThrownBy(() -> application.accept(NOW)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> application.reject(NOW)).isInstanceOf(ConflictException.class);
        assertThat(application.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"REJECTED", "WITHDRAWN", "DECLINED"})
    void finishedApplicationCannotBeWithdrawn(ApplicationStatus status) {
        ActivityApplication application = withStatus(status);

        assertThatThrownBy(() -> application.withdraw(NOW)).isInstanceOf(ConflictException.class);
        assertThat(application.getStatus()).isEqualTo(status);
    }

    @Test
    void pendingInvitationCanBeAcceptedOrDeclined() {
        ActivityApplication accepted = newInvitation();
        accepted.requireOrigin(ApplicationOrigin.INVITATION);
        accepted.accept(NOW);
        assertThat(accepted.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);

        ActivityApplication declined = newInvitation();
        declined.decline(NOW.plusHours(1));
        assertThat(declined.getStatus()).isEqualTo(ApplicationStatus.DECLINED);
        assertThat(declined.getDecidedAt()).isEqualTo(NOW.plusHours(1));
    }

    @Test
    void invitationIsNotRejectedOrWithdrawnWhilePending() {
        ActivityApplication invitation = newInvitation();

        assertThatThrownBy(() -> invitation.reject(NOW)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> invitation.withdraw(NOW))
                .isInstanceOf(ConflictException.class).hasMessageContaining("decline");
        assertThatThrownBy(() -> invitation.requireOrigin(ApplicationOrigin.APPLICATION))
                .isInstanceOf(ConflictException.class);
        assertThat(invitation.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void acceptedInvitationCanBeWithdrawn() {
        ActivityApplication invitation = withStatus(newInvitation(), ApplicationStatus.ACCEPTED);

        invitation.withdraw(NOW);

        assertThat(invitation.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void applicationCannotBeDeclined() {
        ActivityApplication application = newApplication();

        assertThatThrownBy(() -> application.decline(NOW)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> application.requireOrigin(ApplicationOrigin.INVITATION))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void onlyPendingInvitationCanBeDeclined(ApplicationStatus status) {
        ActivityApplication invitation = withStatus(newInvitation(), status);

        assertThatThrownBy(() -> invitation.decline(NOW)).isInstanceOf(ConflictException.class);
        assertThat(invitation.getStatus()).isEqualTo(status);
    }

    private static ActivityApplication newApplication() {
        return new ActivityApplication(null, null, ApplicationOrigin.APPLICATION, NOW);
    }

    private static ActivityApplication newInvitation() {
        return new ActivityApplication(null, null, ApplicationOrigin.INVITATION, NOW);
    }

    private static ActivityApplication withStatus(ApplicationStatus status) {
        return withStatus(newApplication(), status);
    }

    // The status has no setter on purpose; tests reach other statuses by reflection.
    private static ActivityApplication withStatus(ActivityApplication application, ApplicationStatus status) {
        try {
            Field field = ActivityApplication.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(application, status);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return application;
    }
}
