// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.rendering.nui.layers.hud;

import org.joml.Math;

//TODO: move this into the engine (next to `Time` class?) Should we use float (seconds) or long (ms)?
public class TimingInformation {
    /**
     * In-game time (in seconds) for starting the timing.
     */
    public final float start;

    /**
     * In-game time (in seconds) for ending the timing.
     */
    public final float end;

    public TimingInformation(float start, float end) {
        this.start = start;
        this.end = end;
    }

    public float duration() {
        return end - start;
    }

    public float lerp(float currentTime) {
        return Math.lerp(0, 1, currentTime / duration());
    }
}
