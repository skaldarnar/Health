package org.terasology.logic.health;

import org.terasology.entitySystem.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class DamageResistComponent implements Component {
   private int value;
   Map<String,Integer> damageTypes;

    public DamageResistComponent() {
    damageTypes=new HashMap<>(100);
    }
    public DamageResistComponent(int value) {
        this.value=value;
        damageTypes=new HashMap<>(100);
        this.setAll(0);


    }
    public int getValue(){
        return value;
    }
    public void setAll(int d){
        damageTypes.put("directDamage",d);
        damageTypes.put("drowningDamage",d);
        damageTypes.put("explosiveDamage",d);
        damageTypes.put("healingDamage",d);
        damageTypes.put("physicalDamage",d);
        damageTypes.put("supportRemovedDamage",d);

    }

}
