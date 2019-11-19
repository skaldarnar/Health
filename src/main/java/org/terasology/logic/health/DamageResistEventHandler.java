package org.terasology.logic.health;

import javafx.event.EventHandler;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.CharacterSystem;
import org.terasology.logic.health.HealthComponent;
import org.terasology.logic.health.event.BeforeDamagedEvent;
import java.util.ArrayList;
import java.util.List;

import java.sql.SQLOutput;
import java.util.Iterator;

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
@RegisterSystem
public class DamageResistEventHandler extends BaseComponentSystem {

    @ReceiveEvent(components = {DamageResistComponent.class},priority = EventPriority.PRIORITY_HIGH)
    public void suitingOn(BeforeDamagedEvent event, EntityRef entity) {
        String damageType=event.getDamageType().getName();
        String subString=damageType.substring(7);


        DamageResistComponent resistance=entity.getComponent(DamageResistComponent.class);
        float data=100-resistance.damageTypes.get(subString);

            event.multiply(data / 100);
    }
}
