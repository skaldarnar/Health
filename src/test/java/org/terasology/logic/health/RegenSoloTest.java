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
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.moduletestingenvironment.MTEExtension;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.registry.In;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MTEExtension.class)
@Dependencies({"Health"})
public class RegenSoloTest {

    @In
    protected EntityManager entityManager;
    @In
    protected Time time;
    @In
    protected ModuleTestingHelper helper;

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

        // 1 sec wait before regen, 5 secs for regen, 0.2 sec for padding.
        float tick = time.getGameTime() + 6 + 0.200f;
        helper.runWhile(() -> time.getGameTime() <= tick);

        assertEquals(healthComponent.currentHealth, 100);
    }
}
