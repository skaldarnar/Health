// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.logic.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockDamageRendererTest {

    BlockDamageRenderer blockDamageRenderer = new BlockDamageRenderer();

    static HealthComponent healthComponent(int current, int max) {
        HealthComponent health = new HealthComponent();
        health.maxHealth = max;
        health.currentHealth = current;
        return health;
    }

    static String display(HealthComponent component) {
        return "Health(" + component.currentHealth + "/" + component.maxHealth + ")";
    }

    static Stream<Arguments> validHealthComponents() {
        return Stream.of(
                Arguments.of(0, healthComponent(120, 100)),
                Arguments.of(0, healthComponent(100, 100)),
                Arguments.of(0, healthComponent(96, 100)),
                Arguments.of(1, healthComponent(95, 100)),
                Arguments.of(1, healthComponent(90, 100)),
                Arguments.of(9, healthComponent(6, 100)),
                Arguments.of(10, healthComponent(5, 100)),
                Arguments.of(10, healthComponent(0, 100))
        );
    }

    static Stream<Arguments> invalidHealthComponents() {
        return Stream.of(
                Arguments.of(healthComponent(-20, 100)),
                Arguments.of(healthComponent(-20, -100)),
                Arguments.of(healthComponent(20, -100))
        );
    }

    @DisplayName("getDamageEffectsNumber should")
    @Nested
    class DamageEffectsNumber {
        @ParameterizedTest
        @MethodSource("org.terasology.logic.health.BlockDamageRendererTest#validHealthComponents")
        @DisplayName("yield correct damage effect number on valid health component")
        void correctOnValidHealthComponent(int expected, HealthComponent health) {
            assertEquals(expected, blockDamageRenderer.getDamageEffectsNumber(health),
                    "unexpected damage effect number for " + display(health));
        }

        @ParameterizedTest
        @MethodSource("org.terasology.logic.health.BlockDamageRendererTest#invalidHealthComponents")
        @DisplayName("throw exception on invalid health component")
        void exceptionOnInvalidHealthComponent(HealthComponent health) {
            assertThrows(IllegalArgumentException.class, () -> blockDamageRenderer.getDamageEffectsNumber(health),
                    "expected IllegalArgumentException for " + display(health));
        }

    }
}