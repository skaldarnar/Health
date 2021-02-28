// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.logic.health.event;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.network.OwnerEvent;

/**
 * This event is sent after damage has been dealt to an entity.
 */
@OwnerEvent
public class OnDamagedEvent extends HealthChangedEvent {
    private Prefab damageType;
    private int fullAmount;

    public OnDamagedEvent() {
    }

    public OnDamagedEvent(int fullAmount, int change, Prefab damageType, EntityRef instigator) {
        super(instigator, change);
        this.fullAmount = fullAmount;
        this.damageType = damageType;
    }

    public int getDamageAmount() {
        return fullAmount;
    }

    public Prefab getType() {
        return damageType;
    }

}
