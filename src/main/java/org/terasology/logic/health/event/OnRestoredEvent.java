// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.logic.health.event;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.network.OwnerEvent;

/**
 * This event is sent after the entity is restored. Final event of restoration logic.
 */
@OwnerEvent
public class OnRestoredEvent extends HealthChangedEvent {
    /**
     * The amount by which the entity is restored.
     */
    private int amount;

    public OnRestoredEvent() {
    }

    public OnRestoredEvent(int amount, EntityRef instigator) {
        super(instigator, amount);
        this.amount = amount;
    }

    public int getRegenAmount() {
        return amount;
    }

}
