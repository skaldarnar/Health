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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.health.event.BeforeRestoreEvent;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.health.event.DoRestoreEvent;
import org.terasology.logic.health.event.OnFullyHealedEvent;
import org.terasology.logic.health.event.OnRestoredEvent;
import org.terasology.logic.players.event.OnPlayerRespawnedEvent;
import org.terasology.math.TeraMath;

@RegisterSystem(RegisterMode.AUTHORITY)
public class RestorationAuthoritySystem extends BaseComponentSystem {

    @ReceiveEvent
    public void onRestore(DoRestoreEvent event, EntityRef entity, HealthComponent health) {
        BeforeRestoreEvent beforeRestoreEvent = entity.send(new BeforeRestoreEvent(event.getAmount(), event.getEntity()));
        if (!beforeRestoreEvent.isConsumed()) {
            int modifiedRestoreAmount = TeraMath.floorToInt(beforeRestoreEvent.getResultValue());
            if (modifiedRestoreAmount > 0) {
                restore(entity, health, modifiedRestoreAmount);
            } else {
                entity.send(new DoDamageEvent(-modifiedRestoreAmount));
            }
        }
    }

    private void restore(EntityRef entity, HealthComponent health, int restoreAmount) {
        int cappedHealth = Math.min(health.maxHealth, health.currentHealth + restoreAmount);
        int cappedRestoreAmount = cappedHealth - health.currentHealth;
        health.currentHealth = cappedHealth;
        entity.saveComponent(health);
        entity.send(new OnRestoredEvent(cappedRestoreAmount, entity));
        if (cappedHealth == health.maxHealth) {
            entity.send(new OnFullyHealedEvent(entity));
        }
    }

    @ReceiveEvent
    public void onRespawn(OnPlayerRespawnedEvent event, EntityRef entity, HealthComponent healthComponent) {
        healthComponent.currentHealth = healthComponent.maxHealth;
        entity.saveComponent(healthComponent);
    }
}
