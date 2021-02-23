// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.logic.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.health.event.BeforeRestoreEvent;
import org.terasology.logic.health.event.DoRestoreEvent;
import org.terasology.logic.health.event.RestoreFullHealthEvent;
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
public class RestoreTest {

    @In
    private EntityManager entityManager;
    @In
    protected ModuleTestingHelper helper;

    @Test
    public void restoreTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 0;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoRestoreEvent(10));
        assertEquals(10, player.getComponent(HealthComponent.class).currentHealth);

        player.send(new DoRestoreEvent(999));
        assertEquals(100, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restoreEventSentTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 0;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        try (TestEventReceiver<BeforeRestoreEvent> receiver = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class)) {
            List<BeforeRestoreEvent> list = receiver.getEvents();
            assertTrue(list.isEmpty());

            player.send(new DoRestoreEvent(10));
            assertEquals(1, list.size());
        }
    }

    @Test
    public void restoreEventCancelTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 0;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        try (TestEventReceiver<BeforeRestoreEvent> ignored =
                     new TestEventReceiver<>(helper.getHostContext(), BeforeRestoreEvent.class,
                             (event, entity) -> {
            event.consume();
        })) {
            player.send(new DoRestoreEvent(10));
        }
        assertEquals(0, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restoreModifyEventTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 0;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        try (TestEventReceiver<BeforeRestoreEvent> ignored = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class,
                (event, entity) -> {
                    event.add(10);
                    event.add(-5);
                    event.multiply(2);
                })) {
            // Expected value is ( initial:10 + 10 - 5 ) * 2 == 30
            player.send(new DoRestoreEvent(10));
        }
        assertEquals(30, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restoreNegativeTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 50;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoRestoreEvent(-10));
        // Negative base restore value is ignored in the Restoration system
        assertEquals(50, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restoreFullHealthTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 50;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new RestoreFullHealthEvent(player));

        assertEquals(100, player.getComponent(HealthComponent.class).currentHealth);
    }

}
