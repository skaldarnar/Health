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

import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.terasology.entitySystem.Component;
import org.terasology.math.TeraMath;
import org.terasology.network.Replicate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.terasology.logic.health.RegenAuthoritySystem.BASE_REGEN;

public class RegenComponent implements Component {
    @Replicate
    public long soonestEndTime = Long.MAX_VALUE;
    @Replicate
    public Map<String, Float> regenValue = new HashMap<>();
    @Replicate
    public SortedSetMultimap<Long, String> regenEndTime = TreeMultimap.create(Ordering.natural(),
            Ordering.arbitrary());
    @Replicate
    public float remainder;

    public Long findSoonestEndTime() {
        Long endTime = 0L;
        Iterator<Long> iterator = regenEndTime.keySet().iterator();
        while (iterator.hasNext()) {
            endTime = iterator.next();
            if (endTime > 0) {
                return endTime;
            }
        }
        return endTime;
    }

    public void addRegen(String id, float value, long endTime) {
        removeRegen(id);
        regenValue.put(id, value);
        regenEndTime.put(endTime, id);
        soonestEndTime = findSoonestEndTime();
    }

    public void removeRegen(String id) {
        Long removeKey = 0L;
        for (Long key : regenEndTime.keySet()) {
            for (String value : regenEndTime.get(key)) {
                if (id.equals(value)) {
                    removeKey = key;
                    break;
                }
            }
        }
        regenEndTime.remove(removeKey, id);
        regenValue.remove(id);
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
