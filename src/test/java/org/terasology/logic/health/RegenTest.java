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

import com.google.api.client.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.health.event.ActivateRegenEvent;
import org.terasology.logic.health.event.DeactivateRegenEvent;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegenTest extends ModuleTestingEnvironment {

    private EntityManager entityManager;
    private Time time;

    @Override
    public Set<String> getDependencies() {
        Set<String> modules = Sets.newHashSet();
        modules.add("Health");
        return modules;
    }

    @Before
    public void initialize() {
        entityManager = getHostContext().get(EntityManager.class);
        time = getHostContext().get(Time.class);
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
        assertEquals(healthComponent.currentHealth, 95);

        // 1 sec wait before regen, 5 secs for regen, 0.1 sec for padding.
        float tick = time.getGameTime() + 6 + 0.100f;
        runWhile(()-> time.getGameTime() <= tick);

        assertEquals(healthComponent.currentHealth, 100);
    }

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
        runWhile(()-> time.getGameTime() <= tick);

        assertEquals(90, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void multipleRegenTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 100;
        healthComponent.maxHealth = 100;
        healthComponent.waitBeforeRegen = 1;
        healthComponent.regenRate = 1;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new ActivateRegenEvent("Potion#1", 5, 5));
        player.send(new ActivateRegenEvent("Potion#2", 2, 10));

        assertEquals(7, player.getComponent(RegenComponent.class).getRegenValue());

        float tick = time.getGameTime() + 5 + 0.200f;
        runWhile(()-> time.getGameTime() <= tick);

        assertEquals(2, player.getComponent(RegenComponent.class).getRegenValue());
    }
}
