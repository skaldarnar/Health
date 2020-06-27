// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.logic.health.event;

import org.terasology.entitySystem.event.Event;

public class OnMaxHealthChangedEvent implements Event {
    private final int newValue;
    private final int oldValue;

    /**
     * Create a new notification event on character scaling.
     *
     * @param oldValue the entity's old height
     * @param newValue the entity's new height (must be greater zero)
     */
    public OnMaxHealthChangedEvent(final int oldValue, final int newValue) {
        if (newValue <= 0) {
            throw new IllegalArgumentException("zero or negative value");
        }
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * The old max Health previous to scaling.
     */
    public int getOldValue() {
        return oldValue;
    }

    /**
     * The new max Health after scaling.
     */
    public int getNewValue() {
        return newValue;
    }

    /**
     * The scaling factor determined by the quotient of new and old value.
     * <p>
     * This is guaranteed to be greater zero (> 0).
     */
    public float getFactor() {
        return (float) newValue / oldValue;
    }
}

