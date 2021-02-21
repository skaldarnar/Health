/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
@Dependencies({"Health"})
public class RestorationTest {

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
        assertEquals(player.getComponent(HealthComponent.class).currentHealth, 10);

        player.send(new DoRestoreEvent(999));
        assertEquals(player.getComponent(HealthComponent.class).currentHealth, 100);
    }

    @Test
    public void restoreEventSentTest() {
        TestEventReceiver<BeforeRestoreEvent> receiver = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class);
        List<BeforeRestoreEvent> list = receiver.getEvents();
        assertTrue(list.isEmpty());

        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 0;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoRestoreEvent(10));
        assertEquals(1, list.size());
    }

    @Test
    public void restoreEventCancelTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 0;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        TestEventReceiver<BeforeRestoreEvent> receiver = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class,
                (event, entity) -> {
                    event.consume();
                });
        player.send(new DoRestoreEvent(10));
        assertEquals(0, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restorationModifyEventTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 0;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        TestEventReceiver<BeforeRestoreEvent> receiver = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class,
                (event, entity) -> {
                    event.add(5);
                    event.multiply(2);
                });
        // Expected value is ( initial:10 + 5 ) * 2 == 30
        player.send(new DoRestoreEvent(10));
        assertEquals(30, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restorationNegativeTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 50;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoRestoreEvent(-10));
        // No healing happens when base restoration amount is negative, with no negative multipliers.
        assertEquals(50, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void restorationNegativeModifyEventTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 50;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        TestEventReceiver<BeforeRestoreEvent> receiver = new TestEventReceiver<>(helper.getHostContext(),
                BeforeRestoreEvent.class,
                (event, entity) -> {
                    event.multiply(-2);
                });
        // Expected restoration value is ( initial:10 ) * (-2) == (-20)
        // So, final health value: 50 + (-20) == 30
        player.send(new DoRestoreEvent(10));
        assertEquals(30, player.getComponent(HealthComponent.class).currentHealth);
    }
}
