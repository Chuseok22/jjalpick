package com.chuseok22.mimefit.common.core.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-21T01:02:03Z");

    private static class SampleEntity extends BaseEntity {

    }

    @Test
    void newEntity_isNotDeleted() {
        SampleEntity sample = new SampleEntity();

        assertThat(sample.isDeleted()).isFalse();
        assertThat(sample.getDeletedAt()).isNull();
    }

    @Test
    void delete_marksDeletedWithClockInstant() {
        SampleEntity sample = new SampleEntity();

        sample.delete(Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        assertThat(sample.isDeleted()).isTrue();
        assertThat(sample.getDeletedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void delete_calledTwice_keepsFirstDeletedAt() {
        SampleEntity sample = new SampleEntity();
        sample.delete(Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        sample.delete(Clock.fixed(FIXED_INSTANT.plusSeconds(60), ZoneOffset.UTC));

        assertThat(sample.getDeletedAt()).isEqualTo(FIXED_INSTANT);
    }

}
