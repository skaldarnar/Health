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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.health.event.DoHealEvent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;

import static org.junit.Assert.assertEquals;
import java.util.Set;

public class HealEventTest extends ModuleTestingEnvironment {

    private EntityManager entityManager;
    private Time time;

    private static final Logger logger = LoggerFactory.getLogger(HealEventTest.class);

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
    public void healTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 0;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoHealEvent(10));
        assertEquals(player.getComponent(HealthComponent.class).currentHealth, 10);

        player.send(new DoHealEvent(999));
        assertEquals(player.getComponent(HealthComponent.class).currentHealth, 100);
    }

}
