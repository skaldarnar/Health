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
package org.terasology.logic.health.event;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.Event;

/**
 * The event sent to restore health to entity. Starting point of Restoration cycle.
 */
public class DoRestoreEvent implements Event {
    /** The amount of health points being restored. */
    private int amount;
    /** The entity that caused the restoration */
    private EntityRef instigator;

    public DoRestoreEvent(int amount) {
        this(amount, EntityRef.NULL);
    }

    public DoRestoreEvent(int amount, EntityRef entity) {
        this.amount = amount;
        this.instigator = entity;
    }

    public int getAmount() {
        return amount;
    }

    public EntityRef getInstigator() {
        return instigator;
    }
}
