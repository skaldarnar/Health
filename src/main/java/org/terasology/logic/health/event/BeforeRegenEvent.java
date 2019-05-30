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
import org.terasology.entitySystem.event.AbstractConsumableValueModifiableEvent;

/**
 * This event is sent to an entity to allow modification and cancellation of regen.
 *
 * Modifications are accumulated as modifiers (additions), multipliers and postModifiers (additions after multipliers).
 */
public class BeforeRegenEvent extends AbstractConsumableValueModifiableEvent {
    /** The entity which is being regenerated. */
    private EntityRef entity;

    /**
     * Constructor to create a new BeforeRegenEvent.
     *
     * @param amount    The amount by which the entity is regenerated.
     * @param entity    The entity which is being regenerated.
     */
    public BeforeRegenEvent(int amount, EntityRef entity) {
        super(amount);
        this.entity = entity;
    }

    /**
     * Method to get the EntityRef of entity being regenerated.
     * @return Entity being regenerated.
     */
    public EntityRef getEntity() {
        return entity;
    }

}
