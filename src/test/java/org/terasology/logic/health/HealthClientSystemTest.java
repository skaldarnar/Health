// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.logic.health;

import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.internal.PojoEntityManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Direction;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthClientSystemTest {

    static LocationComponent location;
    static HealthClientSystem healthClientSystem;

    @BeforeAll
    static void setup() {
        location = new LocationComponent(new Vector3f(0, 0, 0));
        healthClientSystem = new HealthClientSystem();
    }

    static Stream<Arguments> compassDirections() {
        return Stream.of(
                Arguments.of(Direction.FORWARD),
                Arguments.of(Direction.RIGHT),
                Arguments.of(Direction.BACKWARD),
                Arguments.of(Direction.LEFT)
        );
    }

    @ParameterizedTest
    @MethodSource("compassDirections")
    void determineDamageDirectionForCompassDirections(Direction direction) {

        EntityRef instigator = new PojoEntityManager().create();
        instigator.addComponent(new LocationComponent(direction.asVector3f()));

        Direction damageDirection = healthClientSystem.determineDamageDirection(instigator, location);

        assertEquals(direction, damageDirection);
    }
}
