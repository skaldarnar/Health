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

import gnu.trove.iterator.TFloatIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.health.event.BeforeHealEvent;
import org.terasology.logic.health.event.DoHealEvent;
import org.terasology.logic.health.event.FullHealthEvent;
import org.terasology.logic.health.event.OnHealedEvent;
import org.terasology.logic.players.event.OnPlayerRespawnedEvent;
import org.terasology.math.TeraMath;
import org.terasology.registry.In;

/**
 * This system takes care of healing of entities with HealthComponent.
 * To increase the health of an entity, send DoHealEvent
 *
 * Logic flow for healing:
 * - DoHealEvent
 * - BeforeHealEvent
 * - (HealthComponent saved)
 * - OnHealedEvent
 * - FullHealthEvent (if at full health)
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class HealthAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(HealthAuthoritySystem.class);

    @In
    private EntityManager entityManager;

    @In
    private org.terasology.engine.Time time;

    @Override
    public void update(float delta) {
        for (EntityRef entity : entityManager.getEntitiesWith(HealthComponent.class)) {
            HealthComponent health = entity.getComponent(HealthComponent.class);
            if (health.currentHealth <= 0) {
                continue;
            }

            if (health.currentHealth == health.maxHealth || health.regenRate == 0) {
                continue;
            }

            int healAmount = 0;
            healAmount = regenerateHealth(health, healAmount);

            checkHealed(entity, health, healAmount);
        }
    }

    private int regenerateHealth(HealthComponent health, int healAmount) {
        int newHeal = healAmount;
        while (time.getGameTimeInMs() >= health.nextRegenTick) {
            newHeal++;
            health.nextRegenTick = health.nextRegenTick + (long) (1000 / health.regenRate);
        }
        return newHeal;
    }

    private void checkHealed(EntityRef entity, HealthComponent health, int healAmount) {
        if (healAmount > 0) {
            checkHeal(entity, healAmount, entity);
            entity.saveComponent(health);
        }
    }

    private void checkHeal(EntityRef entity, int healAmount, EntityRef instigator) {
        BeforeHealEvent beforeHeal = entity.send(new BeforeHealEvent(healAmount, instigator));
        if (!beforeHeal.isConsumed()) {
            int modifiedAmount = calculateTotal(beforeHeal.getBaseHeal(), beforeHeal.getMultipliers(), beforeHeal.getModifiers());
            if (modifiedAmount > 0) {
                doHeal(entity, modifiedAmount, instigator);
            } else if (modifiedAmount < 0) {
                // TODO: Add damage events
                // doDamage(entity, -modifiedAmount, EngineDamageTypes.HEALING.get(), instigator, EntityRef.NULL);
            }
        }
    }

    private void doHeal(EntityRef entity, int healAmount, EntityRef instigator) {
        HealthComponent health = entity.getComponent(HealthComponent.class);
        if (health != null) {
            int healedAmount = Math.min(health.currentHealth + healAmount, health.maxHealth) - health.currentHealth;
            health.currentHealth += healedAmount;
            entity.saveComponent(health);
            entity.send(new OnHealedEvent(healAmount, healedAmount, instigator));
            if (health.currentHealth == health.maxHealth) {
                entity.send(new FullHealthEvent(instigator));
            }
        }
    }

    private int calculateTotal(int base, TFloatList multipliers, TIntList modifiers) {
        // For now, add all modifiers and multiply by all multipliers. Negative modifiers cap to zero, but negative
        // multipliers remain (so damage can be flipped to healing)

        float total = base;
        TIntIterator modifierIter = modifiers.iterator();
        while (modifierIter.hasNext()) {
            total += modifierIter.next();
        }
        total = Math.max(0, total);
        if (total == 0) {
            return 0;
        }
        TFloatIterator multiplierIter = multipliers.iterator();
        while (multiplierIter.hasNext()) {
            total *= multiplierIter.next();
        }
        return TeraMath.floorToInt(total);

    }

    /** Handles DoHeal event to increase health of entity */
    @ReceiveEvent(components = {HealthComponent.class})
    public void onHeal(DoHealEvent event, EntityRef entity) {
        checkHeal(entity, event.getAmount(), event.getInstigator());
    }

    /** Resets the health of player to maxHealth on re-spawn. */
    @ReceiveEvent
    public void onRespawn(OnPlayerRespawnedEvent event, EntityRef entity, HealthComponent healthComponent) {
        healthComponent.currentHealth = healthComponent.maxHealth;
        entity.saveComponent(healthComponent);
    }
}
