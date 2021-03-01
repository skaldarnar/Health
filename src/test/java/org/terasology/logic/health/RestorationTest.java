// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.logic.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.health.event.BeforeRestoreEvent;
import org.terasology.logic.health.event.DoRestoreEvent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.moduletestingenvironment.MTEExtension;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.moduletestingenvironment.TestEventReceiver;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.registry.In;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MTEExtension.class)
@Dependencies("Health")
public class RestorationTest {

    @In
    private EntityManager entityManager;
    @In
    protected ModuleTestingHelper helper;

    private EntityRef newPlayer(int currentHealth) {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = currentHealth;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);
        return player;
    }

    @Test
    public void restoreTest() {
        final EntityRef player = newPlayer(0);

        player.send(new DoRestoreEvent(10));
        assertEquals(10, player.getComponent(HealthComponent.class).currentHealth);

        player.send(new DoRestoreEvent(999));
        assertEquals(100, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restoreEventSentTest() {
        try (TestEventReceiver<BeforeRestoreEvent> receiver = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class)) {
            List<BeforeRestoreEvent> list = receiver.getEvents();
            assertTrue(list.isEmpty());

            final EntityRef player = newPlayer(0);

            player.send(new DoRestoreEvent(10));
            assertEquals(1, list.size());
        }
    }

    @Test
    public void restoreEventCancelTest() {
        final EntityRef player = newPlayer(0);

        try (TestEventReceiver<BeforeRestoreEvent> ignored = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class,
                (event, entity) -> event.consume())) {
            player.send(new DoRestoreEvent(10));
        }
        assertEquals(0, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restorationModifyEventTest() {
        final EntityRef player = newPlayer(0);

        try (TestEventReceiver<BeforeRestoreEvent> ignored = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class,
                (event, entity) -> {
                    event.add(5);
                    event.multiply(2);
                })) {
            // Expected value is ( initial:10 + 5 ) * 2 == 30
            player.send(new DoRestoreEvent(10));
        }
        assertEquals(30, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restorationNegativeTest() {
        final EntityRef player = newPlayer(50);

        player.send(new DoRestoreEvent(-10));
        // No healing happens when base restoration amount is negative, with no negative multipliers.
        assertEquals(50, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restorationNegativeModifyEventTest() {
        final EntityRef player = newPlayer(50);

        try (TestEventReceiver<BeforeRestoreEvent> ignored = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class,
                (event, entity) -> {
                    event.multiply(-2);
                })) {
            // Expected restoration value is ( initial:10 ) * (-2) == (-20)
            // As AbstractValueModifiableEvent#getResultValue guarantees that the resulting value is non-negative, this
            // is actually canceled out to be 0...
            player.send(new DoRestoreEvent(10));
        }
        assertEquals(50, player.getComponent(HealthComponent.class).currentHealth);
    }
}
