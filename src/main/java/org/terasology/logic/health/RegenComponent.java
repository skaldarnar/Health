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
import org.terasology.math.TeraMath;
import org.terasology.network.Replicate;

import java.util.HashMap;
import java.util.Map;

import static org.terasology.logic.health.RegenAuthoritySystem.BASE_REGEN;

public class RegenComponent implements Component {
    @Replicate
    public long soonestEndTime;
    @Replicate
    public Map<String, Float> regenValue = new HashMap<>();
    @Replicate
    public Map<String, Long> regenEndTime = new HashMap<>();
    @Replicate
    public float remainder;

    public Long findSoonestEndTime() {
        long result = Long.MAX_VALUE;
        for (long value : regenEndTime.values()) {
            if (value == -1) {
                continue;
            }
            result = Math.min(result, value);
        }
        return result;
    }

    public void addRegen(String id, float value, long endTime) {
        if (value != 0) {
            regenValue.put(id, value);
            regenEndTime.put(id, endTime);
            if (endTime > 0) {
                soonestEndTime = Math.min(soonestEndTime, endTime);
            }
        }
    }

    public void removeRegen(String id) {
        final Long removedEndTime = regenEndTime.remove(id);
        regenValue.remove(id);
        if (removedEndTime == soonestEndTime) {
            soonestEndTime = findSoonestEndTime();
        }
    }

    public int getRegenValue() {
        float totalValue = remainder;
        for (float value : regenValue.values()) {
            totalValue += value;
        }
        totalValue = Math.max(0, totalValue);
        remainder = totalValue % 1;
        return TeraMath.floorToInt(totalValue);
    }

    public boolean hasBaseRegenOnly() {
        return (regenValue.size() == 1) && (regenValue.containsKey(BASE_REGEN));
    }
}
