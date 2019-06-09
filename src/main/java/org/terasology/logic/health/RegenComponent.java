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

public class RegenComponent implements Component {
    private Map<String, Float> regenValue = new HashMap<>();
    private Map<String, Long> regenEndTime = new HashMap<>();
    public long lowestEndTime;

    public Long findLowestEndTime() {
        long result = Long.MAX_VALUE;
        for (long value : regenEndTime.values()) {
            result = Math.min(result, value);
        }
        return lowestEndTime;
    }

    public long getLowestEndTime() {
        return lowestEndTime;
    }

    public void addRegen(String id, float value, long endTime) {
        regenValue.put(id, value);
        regenEndTime.put(id, endTime);
        lowestEndTime = Math.min(lowestEndTime, endTime);
    }

    public void removeRegen(String id) {
        final Long removedEndTime = regenEndTime.remove(id);
        regenValue.remove(id);
        if (removedEndTime == lowestEndTime) {
            lowestEndTime = findLowestEndTime();
        }
    }

    public void removeCompleted(long currentTime) {
        long endTime;
        for (String id: regenEndTime.keySet()) {
            endTime = regenEndTime.get(id);
            if (endTime < currentTime) {
                regenEndTime.remove(id);
                regenValue.remove(id);
            }
        }
        lowestEndTime = findLowestEndTime();
    }

    public boolean isEmpty() {
        return regenValue.isEmpty();
    }

    public int getRegenValue() {
        float totalValue = 0;
        for (float value: regenValue.values()) {
            totalValue += value;
        }
        if (totalValue < 0) {
            totalValue = 0;
        }
        return TeraMath.floorToInt(totalValue);
    }

}
