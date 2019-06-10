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
import org.terasology.logic.health.event.BeforeRegenEvent;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;
import org.terasology.moduletestingenvironment.TestEventReceiver;

import java.util.List;
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
    public void beforeRegenEventTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 100;
        healthComponent.maxHealth = 100;

        RegenComponent regenComponent = new RegenComponent();
        healthComponent.waitBeforeRegen = 1;
        healthComponent.regenRate = 1;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);
        player.addComponent(regenComponent);

        TestEventReceiver<BeforeRegenEvent> receiver = new TestEventReceiver<>(getHostContext(), BeforeRegenEvent.class);
        List<BeforeRegenEvent> list = receiver.getEvents();
        assertTrue(list.isEmpty());

        player.send(new DoDamageEvent(5));
        assertEquals(healthComponent.currentHealth, 95);

        // 1 sec wait before regen, 5 secs for regen, 0.1 sec for padding.
        float tick = time.getGameTime() + 6 + 0.100f;
        runWhile(()-> time.getGameTime() <= tick);

        assertEquals(5, list.size());
        assertEquals(healthComponent.currentHealth, 100);
    }

    @Test
    public void regenEventCancelTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 100;
        healthComponent.maxHealth = 100;

        RegenComponent regenComponent = new RegenComponent();
        healthComponent.waitBeforeRegen = 1;
        healthComponent.regenRate = 1;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);
        player.addComponent(regenComponent);

        TestEventReceiver<BeforeRegenEvent> receiver = new TestEventReceiver<>(getHostContext(),
                BeforeRegenEvent.class,
                (event, entity) -> {
                    event.consume();
                });
        List<BeforeRegenEvent> list = receiver.getEvents();

        player.send(new DoDamageEvent(10));
        assertTrue(list.isEmpty());

        float tick = time.getGameTime() + 1 + 0.100f;
        runWhile(()-> time.getGameTime() <= tick);

        assertEquals(90, player.getComponent(HealthComponent.class).currentHealth);
    }
}
