package com.volunnear.activity;

import com.volunnear.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Status transitions of Q4c, Q4d and Q8a. */
class ActivityTest {

    @Test
    void newActivityIsDraftWithMediumPriority() {
        Activity activity = new Activity();

        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.DRAFT);
        assertThat(activity.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void draftIsPublishedToOpen() {
        Activity activity = new Activity();

        activity.publish();

        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.OPEN);
    }

    @ParameterizedTest
    @EnumSource(value = ActivityStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void onlyDraftCanBePublished(ActivityStatus status) {
        Activity activity = withStatus(status);

        assertThatThrownBy(activity::publish).isInstanceOf(ConflictException.class);
        assertThat(activity.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(value = ActivityStatus.class, names = {"DRAFT", "OPEN"})
    void draftAndOpenCanBeCancelledAndEdited(ActivityStatus status) {
        Activity activity = withStatus(status);

        assertThatCode(activity::requireEditable).doesNotThrowAnyException();
        activity.cancel();
        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = ActivityStatus.class, names = {"IN_PROGRESS", "COMPLETED", "CANCELLED"})
    void otherStatusesCanBeNeitherCancelledNorEdited(ActivityStatus status) {
        Activity activity = withStatus(status);

        assertThatThrownBy(activity::cancel).isInstanceOf(ConflictException.class);
        assertThatThrownBy(activity::requireEditable).isInstanceOf(ConflictException.class);
        assertThat(activity.getStatus()).isEqualTo(status);
    }

    @Test
    void openIsCompleted() {
        Activity activity = withStatus(ActivityStatus.OPEN);

        activity.complete();

        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.COMPLETED);
    }

    @ParameterizedTest
    @EnumSource(value = ActivityStatus.class, names = "OPEN", mode = EnumSource.Mode.EXCLUDE)
    void onlyOpenCanBeCompleted(ActivityStatus status) {
        Activity activity = withStatus(status);

        assertThatThrownBy(activity::complete).isInstanceOf(ConflictException.class);
        assertThat(activity.getStatus()).isEqualTo(status);
    }

    // The status has no setter on purpose; tests reach other statuses by reflection.
    private static Activity withStatus(ActivityStatus status) {
        Activity activity = new Activity();
        try {
            Field field = Activity.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(activity, status);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return activity;
    }
}
