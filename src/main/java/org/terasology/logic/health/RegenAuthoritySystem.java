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

        // If the current time passes the CHECK_INTERVAL threshold, regenerate.
        if (currentTime >= lastUpdated + CHECK_INTERVAL) {
            // Set the lastUpdated time to be the currentTime.
            lastUpdated = currentTime;

            // For every entity with the health component, ignore if it is dead or maxHealth is reached
            // check to see if they have passed a REGENERATION_TICK. If so, regenerate the applicable
            // entities with the given regenAmount.
            for (EntityRef entity : entityManager.getEntitiesWith(HealthComponent.class)) {
                HealthComponent component = entity.getComponent(HealthComponent.class);
                RegenComponent regen = entity.getComponent(RegenComponent.class);

                if (component.currentHealth <= 0) {
                    continue;
                }

                if (component.currentHealth == component.maxHealth || regen.regenRate == 0) {
                    continue;
                }

                long lastRegenTick = regen.nextRegenTick - REGENERATION_TICK;
                if (currentTime >= lastRegenTick + REGENERATION_TICK) {
                    // Calculate this multiplier to account for time delays.
                    int multiplier = (int) (currentTime - lastRegenTick) / REGENERATION_TICK;

                    int amount = TeraMath.floorToInt(regen.regenRate * multiplier);
                    checkRegenerated(entity, component, amount);
                }
            }
        }
    }

    private void checkRegenerated(EntityRef entity, HealthComponent health, int healAmount) {
        if (healAmount > 0) {
            BeforeRegenEvent beforeRegen = entity.send(new BeforeRegenEvent(healAmount, entity));
            if (!beforeRegen.isConsumed()) {
                int modifiedAmount = TeraMath.floorToInt(beforeRegen.getResultValue());
                if (modifiedAmount > 0) {
                    doRegenerate(entity, modifiedAmount, entity);
                }
            }
            entity.saveComponent(health);
        }
    }

    private void doRegenerate(EntityRef entity, int healAmount, EntityRef instigator) {
        HealthComponent health = entity.getComponent(HealthComponent.class);
        RegenComponent component = entity.getComponent(RegenComponent.class);
        if (health != null) {
            int cappedHealth = Math.min(health.currentHealth + healAmount, health.maxHealth);
            int cappedHealAmount = cappedHealth - health.currentHealth;
            health.currentHealth = cappedHealth;
            component.nextRegenTick = time.getGameTimeInMs() + REGENERATION_TICK;
            entity.saveComponent(health);
            entity.send(new OnRegenedEvent(cappedHealAmount, entity));
            if (health.currentHealth == health.maxHealth) {
                entity.send(new OnFullyHealedEvent(instigator));
            }
        }
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
            // DO we need to remove every time? yes.
            regenTimeIterator.remove();
        }

        operationsToInvoke.stream().filter(EntityRef::exists).forEach(regenEntity -> {
            final RegenComponent regenComponent =
                    regenEntity.getComponent(RegenComponent.class);
            // Add a check for health component too?

            // If there is a RegenComponent, proceed. Else report an error to the log.
            if (regenComponent != null) {
                // regen
                // update nextRegenTick
                // Update regenSortedByTime to have next tick
                // save regenComp
            }
        });
    }

    @ReceiveEvent
    public void onRegenComponentAdded(OnActivatedComponent event, EntityRef entity, RegenComponent regen,
                                      HealthComponent health) {
        // add this entity to scheduler (regenSortedByTime)
    }

    @ReceiveEvent
    public void onRegenComponentRemoved(BeforeDeactivateComponent event, EntityRef entity, RegenComponent regen,
                                        HealthComponent health) {
        // remove this entity from scheduler
    }
}
