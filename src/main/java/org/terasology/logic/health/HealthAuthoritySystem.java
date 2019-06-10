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
import org.terasology.logic.health.event.BeforeHealEvent;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.health.event.DoHealEvent;
import org.terasology.logic.health.event.OnFullyHealedEvent;
import org.terasology.logic.health.event.OnHealedEvent;
import org.terasology.logic.players.event.OnPlayerRespawnedEvent;
import org.terasology.math.TeraMath;
import org.terasology.registry.In;

/**
 * This system takes care of healing of entities with HealthComponent.
 * To increase the health of an entity, send DoHealEvent
 * <p>
 * Logic flow for healing:
 * - DoHealEvent
 * - BeforeHealEvent
 * - (HealthComponent saved)
 * - OnHealedEvent
 * - OnFullyHealedEvent (if healed to full health)
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class HealthAuthoritySystem extends BaseComponentSystem {

    private static final Logger logger = LoggerFactory.getLogger(HealthAuthoritySystem.class);

    @In
    private EntityManager entityManager;

    private void checkHeal(EntityRef entity, int healAmount, EntityRef instigator) {
        BeforeHealEvent beforeHeal = entity.send(new BeforeHealEvent(healAmount, instigator));
        if (!beforeHeal.isConsumed()) {
            int modifiedAmount = calculateTotal(beforeHeal.getBaseHeal(), beforeHeal.getMultipliers(), beforeHeal.getModifiers());
            if (modifiedAmount > 0) {
                doHeal(entity, modifiedAmount, instigator);
            } else if (modifiedAmount < 0) {
                entity.send(new DoDamageEvent(-modifiedAmount, EngineDamageTypes.HEALING.get(), instigator, EntityRef.NULL));
            }
        }
    }

    private void doHeal(EntityRef entity, int healAmount, EntityRef instigator) {
        HealthComponent health = entity.getComponent(HealthComponent.class);
        if (health != null) {
            int cappedHealth = Math.min(health.currentHealth + healAmount, health.maxHealth);
            int cappedHealAmount = cappedHealth - health.currentHealth;
            health.currentHealth = cappedHealth;
            entity.saveComponent(health);
            entity.send(new OnHealedEvent(healAmount, cappedHealAmount, instigator));
            if (health.currentHealth == health.maxHealth) {
                entity.send(new OnFullyHealedEvent(instigator));
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

    /**
     * Event handler to increase health of entity.
     *
     * @param event  DoHealEvent which triggered the heal effect.
     * @param entity The entity which will be healed by the event.
     */
    @ReceiveEvent(components = {HealthComponent.class})
    public void onHeal(DoHealEvent event, EntityRef entity) {
        checkHeal(entity, event.getAmount(), event.getInstigator());
    }

    /**
     * Resets the health of player to maxHealth on re-spawn.
     *
     * @param event           OnPlayerRespawnedEvent sent during the re-spwan of player.
     * @param entity          The Player entity.
     * @param healthComponent The health component attached to player entity.
     */
    @ReceiveEvent
    public void onRespawn(OnPlayerRespawnedEvent event, EntityRef entity, HealthComponent healthComponent) {
        healthComponent.currentHealth = healthComponent.maxHealth;
        entity.saveComponent(healthComponent);
    }

}
