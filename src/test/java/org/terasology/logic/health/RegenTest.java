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

@ExtendWith(MTEExtension.class)
@Dependencies({"Health"})
public class RegenTest {

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
        player.send(new DoDamageEvent(10));

        // Deactivate base regen
        player.send(new DeactivateRegenEvent());

        float tick = time.getGameTime() + 1 + 0.100f;
        helper.runWhile(()-> time.getGameTime() <= tick);

        assertEquals(90, player.getComponent(HealthComponent.class).currentHealth);
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
}
