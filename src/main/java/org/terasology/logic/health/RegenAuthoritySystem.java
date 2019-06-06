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
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeRemoveComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.health.event.BeforeRegenEvent;
import org.terasology.logic.health.event.OnFullyHealedEvent;
import org.terasology.logic.health.event.OnRegenedEvent;
import org.terasology.math.TeraMath;
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

    private SortedSetMultimap<Long, EntityRef> regenSortedByTime = TreeMultimap.create(Ordering.natural(),
            Ordering.arbitrary());

    /** Integer storing when to check each effect. */
    private static final int CHECK_INTERVAL = 100;

    /** Integer storing when to apply regen health */
    private static final int REGENERATION_TICK = 1000;

    /** Last time the list of regen effects were checked. */
    private long lastUpdated;

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

    // Regenerates if regenTime is greater than currentTime
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
            final RegenComponent regenComponent =
                    regenEntity.getComponent(RegenComponent.class);
            final HealthComponent health = regenEntity.getComponent(HealthComponent.class);

            // If there is a RegenComponent, proceed.
            if (regenComponent != null && health != null) {
                // regen, happening every 1/RegenRate sec.
                if (health.currentHealth < health.maxHealth) {
                    health.currentHealth += 1;
                    regenEntity.saveComponent(regenComponent);
                    logger.warn("regend " + health.currentHealth);
                    if (health.currentHealth == health.maxHealth) {
                        regenEntity.send(new OnFullyHealedEvent(regenEntity));
                    }
                }
                // update nextRegenTick
                regenComponent.nextRegenTick = regenComponent.nextRegenTick + (long) (1000 / regenComponent.regenRate);
                // Update regenSortedByTime to have next tick
                regenSortedByTime.put(regenComponent.nextRegenTick, regenEntity);
                // save regenComp
                regenEntity.saveComponent(health);
            }
        });
    }

    @ReceiveEvent
    public void onRegenComponentAdded(OnActivatedComponent event, EntityRef entity, RegenComponent regen,
                                      HealthComponent health) {
        regenSortedByTime.put((long) (regen.nextRegenTick + (regen.waitBeforeRegen * 1000)), entity);
    }

    @ReceiveEvent
    public void onRegenComponentRemoved(BeforeDeactivateComponent event, EntityRef entity, RegenComponent regen,
                                        HealthComponent health) {
        regenSortedByTime.remove(regen.nextRegenTick, entity);
    }
}
