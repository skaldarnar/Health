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

import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.health.event.ActivateRegenEvent;
import org.terasology.registry.In;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This system handles the natural regeneration of entities with HealthComponent.
 *
 * Logic flow for Regen:
 * - BeforeRegenEvent
 * - (Health regenerated, HealthComponent saved)
 * - OnRegenedEvent
 * - OnFullyHealedEvent (if healed to full health)

 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class RegenAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(RegenComponent.class);

    // Stores when next to check for new value of regen, contains only entities which are being regenerated
    private SortedSetMultimap<Long, EntityRef> regenSortedByTime = TreeMultimap.create(Ordering.natural(),
            Ordering.arbitrary());

    /** Integer storing when to check each effect. */
    private static final int CHECK_INTERVAL = 100;

    @In
    private Time time;
    @In
    private EntityManager entityManager;

    /**
     * For every update, check to see if the time's been over the CHECK_INTERVAL. If so, verify if a REGENERATION_TICK
     * has passed for every regeneration effect.
     *
     * @param delta The time (in seconds) since the last engine update.
     */
    @Override
    public void update(float delta) {
        final long currentTime = time.getGameTimeInMs();
        // Execute regen schedule
        invokeRegenOperations(currentTime);
    }

    private void invokeRegenOperations(long currentWorldTime) {
        List<EntityRef> operationsToInvoke = new LinkedList<>();
        Iterator<Long> regenTimeIterator = regenSortedByTime.keySet().iterator();
        long processedTime;
        while (regenTimeIterator.hasNext()) {
            processedTime = regenTimeIterator.next();
            if (processedTime > currentWorldTime) {
                break;
            }
            operationsToInvoke.addAll(regenSortedByTime.get(processedTime));
            regenTimeIterator.remove();
        }

        operationsToInvoke.stream().filter(EntityRef::exists).forEach(regenEntity -> {
            // get next endTime and new calculated value, add to regenSortedByTime map
        });

        regenerate();
    }

    private void regenerate() {
        // use regenSortedByTime to regenerate entities
    }

    @ReceiveEvent
    public void onRegenAdded(ActivateRegenEvent event, EntityRef entity, RegenComponent regen,
                             HealthComponent health) {
        addRegenToScheduler(event, entity, regen, health);
    }

    @ReceiveEvent
    public void onRegenAddedWithoutComponent(ActivateRegenEvent event, EntityRef entity, HealthComponent health) {
        RegenComponent regen = new RegenComponent();
        regen.lowestEndTime = Long.MAX_VALUE;
        entity.addComponent(regen);
        addRegenToScheduler(event, entity, regen, health);
    }

    private void addRegenToScheduler(ActivateRegenEvent event, EntityRef entity, RegenComponent regen,
                                     HealthComponent health) {
        if (event.id.equals("baseRegen")) {
            // setting endTime to MAX_VALUE because natural regen happens till entity fully regenerates
            regen.addRegen(event.id, health.regenRate, Long.MAX_VALUE);
            regen.addRegen("wait", -health.regenRate, (long) (health.waitBeforeRegen * 1000));
        } else {
            regen.addRegen(event.id, event.value, (long) (event.endTime * 1000));
        }
        regenSortedByTime.put(regen.getLowestEndTime(), entity);
    }

}
