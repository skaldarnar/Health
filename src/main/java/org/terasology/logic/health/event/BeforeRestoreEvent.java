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

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.AbstractConsumableValueModifiableEvent;

/**
 * This event is sent to an entity to allow modification and cancellation of restoration.
 *
 * Modifications are accumulated as modifiers (additions), multipliers and postModifiers (additions after multipliers).
 */
public class BeforeRestoreEvent extends AbstractConsumableValueModifiableEvent {
    /** The entity which is being restored. */
    private EntityRef entity;

    public BeforeRestoreEvent(int amount, EntityRef entity) {
        super(amount);
        this.entity = entity;
    }

    public EntityRef getEntity() {
        return entity;
    }

}
