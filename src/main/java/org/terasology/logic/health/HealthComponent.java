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

import org.terasology.entitySystem.Component;
import org.terasology.network.Replicate;
import org.terasology.rendering.nui.properties.TextField;

/**
 * Provides Health to entity attached with HealthComponent. Contains the parameters
 * required for all health related events.
 */
public class HealthComponent implements Component {

    /** Maximum allowed health, capped to this if exceeding this value. */
    @Replicate
    public int maxHealth = 20;

    /** Falling speed threshold above which damage is inflicted to entity. */
    @Replicate
    public float fallingDamageSpeedThreshold = 20;

    /** Horizontal speed threshold above which damage is inflicted to entity. */
    @Replicate
    public float horizontalDamageSpeedThreshold = 20;

    /** The multiplier used to calculate damage when horizontal or vertical threshold is crossed. */
    @Replicate
    public float excessSpeedDamageMultiplier = 10f;


    /** The current value of health. */
    @Replicate
    @TextField
    public int currentHealth = 20;

    /** Used to send Destroy event when health breaches zero. */
    public boolean destroyEntityOnNoHealth;

    // Regen Info

    /** Amount of health restored in each regeneration tick. */
    @Replicate
    public float regenRate;

    /** Time delay before regeneration starts. */
    @Replicate
    public float waitBeforeRegen;

    /** Next tick time that will trigger regeneration. */
    public long nextRegenTick;

}
