// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.logic.health;

import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.terasology.entitySystem.Component;
import org.terasology.network.Replicate;

import java.util.HashMap;
import java.util.Map;

/**
 * Not for direct access! Use regen events instead.
 *
 * @see org.terasology.logic.health.event.ActivateRegenEvent
 * @see org.terasology.logic.health.event.DeactivateRegenEvent
 */
public class RegenComponent implements Component {
    /**
     * The timestamp in in-game time (ms) when the next regeneration action ends.
     */
    @Replicate
    public long soonestEndTime = Long.MAX_VALUE;

    /**
     * Mapping from regeneration action ids to the regeneration value.
     */
    @Replicate
    public Map<String, Float> regenValue = new HashMap<>();

    /**
     * Registered regeneration action ids associated to their end time.
     */
    @Replicate
    public SortedSetMultimap<Long, String> regenEndTime = TreeMultimap.create(Ordering.natural(),
            Ordering.arbitrary());

    @Replicate
    public float remainder;
}
