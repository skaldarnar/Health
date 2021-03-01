// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.logic.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.health.event.ActivateRegenEvent;
import org.terasology.logic.health.event.DeactivateRegenEvent;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.moduletestingenvironment.MTEExtension;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.registry.In;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MTEExtension.class)
@Dependencies({"Health"})
public class RegenTest {

    private static final float BUFFER = 0.2f; // 200 ms buffer time

    @In
    protected EntityManager entityManager;
    @In
    protected Time time;
    @In
    protected ModuleTestingHelper helper;


    @Test
    public void regenCancelTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 100;
        healthComponent.maxHealth = 100;
        healthComponent.waitBeforeRegen = 1;
        healthComponent.regenRate = 1;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);
        player.send(new DoDamageEvent(20));

        // Deactivate base regen
        player.send(new DeactivateRegenEvent());
        // there may have been some regeneration between the damage event and the deactivation
        // Thus, we compare against the current health which should be less than the max health.
        int currentHealth = player.getComponent(HealthComponent.class).currentHealth;
        assertTrue(currentHealth < 100, "regeneration should have been canceled before reaching max health");

        float tick = time.getGameTime() + 1 + 0.100f;
        helper.runWhile(()-> time.getGameTime() <= tick);

        assertEquals(currentHealth, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void multipleRegenTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 10;
        healthComponent.maxHealth = 100;
        healthComponent.waitBeforeRegen = 1;
        healthComponent.regenRate = 1;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new ActivateRegenEvent("Potion#1", 5, 5));
        player.send(new ActivateRegenEvent("Potion#2", 2, 10));

        RegenComponent regen = player.getComponent(RegenComponent.class);
        RegenAuthoritySystem system = new RegenAuthoritySystem();
        assertEquals(7, system.getRegenValue(regen));

        float tick = time.getGameTime() + 6 + 0.200f;
        helper.runWhile(()-> time.getGameTime() <= tick);

        regen = player.getComponent(RegenComponent.class);
        assertEquals(2, system.getRegenValue(regen));
    }

    @Test
    public void zeroRegenTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 100;
        healthComponent.maxHealth = 100;
        healthComponent.waitBeforeRegen = 1;
        healthComponent.regenRate = 0;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoDamageEvent(5));
        assertEquals(healthComponent.currentHealth, 95);

        float tick = time.getGameTime() + 2 + 0.500f;
        helper.runWhile(()-> time.getGameTime() <= tick);

        assertFalse(player.hasComponent(RegenComponent.class));
    }

    @Test
    public void regenTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 100;
        healthComponent.maxHealth = 100;
        healthComponent.waitBeforeRegen = 1;
        healthComponent.regenRate = 1;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoDamageEvent(5));
        helper.runWhile(200, () -> true);
        assertEquals(healthComponent.currentHealth, 95);

        // wait 1 second before regen starts
        float regenStarts = time.getGameTime() + healthComponent.waitBeforeRegen + BUFFER;
        helper.runWhile(() -> time.getGameTime() <= regenStarts);
        assertTrue(player.hasComponent(RegenComponent.class));

        // wait for the regeneration to be finished: 5 dmg /  1hp/s = 5 seconds
        float regenEnded = time.getGameTime() + 5f + BUFFER;
        helper.runWhile(() -> time.getGameTime() <= regenEnded);

        assertEquals(healthComponent.currentHealth, 100);
    }
}
