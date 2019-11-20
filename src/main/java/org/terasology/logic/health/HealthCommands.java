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

import org.terasology.assets.ResourceUrn;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.console.commandSystem.annotations.Command;
import org.terasology.logic.console.commandSystem.annotations.CommandParam;
import org.terasology.logic.console.commandSystem.annotations.Sender;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.health.event.DoRestoreEvent;
import org.terasology.logic.permission.PermissionManager;
import org.terasology.network.ClientComponent;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.utilities.Assets;

import java.util.Optional;

@RegisterSystem
@Share(HealthCommands.class)
public class HealthCommands extends BaseComponentSystem {

    @In
    private PrefabManager prefabManager;

    @Command(value = "kill", shortDescription = "Reduce the player's health to zero", runOnServer = true,
            requiredPermission = PermissionManager.NO_PERMISSION)
    public void killCommand(@Sender EntityRef client) {
        ClientComponent clientComp = client.getComponent(ClientComponent.class);
        HealthComponent health = clientComp.character.getComponent(HealthComponent.class);
        if (health != null) {
            clientComp.character.send(new DestroyEvent(clientComp.character, EntityRef.NULL, EngineDamageTypes.DIRECT.get()));
        }
    }

    @Command(shortDescription = "Reduce the player's health by an amount", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String damage(@Sender EntityRef client, @CommandParam("amount") int amount) {
        ClientComponent clientComp = client.getComponent(ClientComponent.class);
        clientComp.character.send(new DoDamageEvent(amount, EngineDamageTypes.DIRECT.get(), clientComp.character));

        return "Inflicted damage of " + amount;
    }

    @Command(shortDescription = "Reduce the player's health by an amount by a specific type of damage", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String damageWithType(@Sender EntityRef client, @CommandParam("damageType") String damageType, @CommandParam("amount") int amount) {
        ClientComponent clientComp = client.getComponent(ClientComponent.class);
        Prefab damageTypePrefab = prefabManager.getPrefab(damageType);
        if (damageTypePrefab != null) {
            clientComp.character.send(new DoDamageEvent(amount, damageTypePrefab, clientComp.character));
            return "Inflicted " + amount + " damage of type " + damageType;
        } else {
            return "Specified damage type does not exist.";
        }
    }
    @Command(shortDescription = "Damage Resistance", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String damageResist(@CommandParam("DamageType") String dType,@CommandParam("Percentage") int value,@Sender EntityRef clientEntity) {
        if(value>=0&&value<=100) {
            ClientComponent player = clientEntity.getComponent(ClientComponent.class);
            DamageResistComponent damageResist = player.character.getComponent(DamageResistComponent.class);
            String s="ALL";
            if(damageResist==null) {
                player.character.addComponent(new DamageResistComponent());
                damageResist = player.character.getComponent(DamageResistComponent.class);
            }
            if(dType.equals(s)){
                damageResist.setAll(value);
                if(value==0){
                    player.character.removeComponent(DamageResistComponent.class);
                }
            }

            if(damageResist.damageTypes.containsKey(dType)){
                damageResist.damageTypes.put(dType,value);
            }
            else{
                if(!dType.equals(s))
                    return "Not a valid Damage Type";
            }
            return "Resistance:"+ value +"% to "+dType;
        }
        else{
            return "accepted values:[0 to 100]";
        }


    }
    @Command(shortDescription = "Check your Resistance", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String checkResistance(@Sender EntityRef clientEntity){
        ClientComponent player=clientEntity.getComponent(ClientComponent.class);
        DamageResistComponent damageResistComponent=player.character.getComponent(DamageResistComponent.class);
        if(damageResistComponent==null){
            return "You don't have Resistance to any type of damage.";
        }
        else{
            return damageResistComponent.damageTypes.entrySet().toString();
        }
    }

    @Command(shortDescription = "Restores your health to max", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String healthMax(@Sender EntityRef clientEntity) {
        ClientComponent clientComp = clientEntity.getComponent(ClientComponent.class);
        clientComp.character.send(new DoRestoreEvent(100000, clientComp.character));
        return "Health fully restored";
    }

    @Command(shortDescription = "Restores your health by an amount", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String heal(@Sender EntityRef client, @CommandParam("amount") int amount) {
        ClientComponent clientComp = client.getComponent(ClientComponent.class);
        clientComp.character.send(new DoRestoreEvent(amount, clientComp.character));
        return "Health restored for " + amount;
    }

    @Command(shortDescription = "Set max health", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String setMaxHealth(@Sender EntityRef client, @CommandParam("max") int max) {
        ClientComponent clientComp = client.getComponent(ClientComponent.class);
        HealthComponent health = clientComp.character.getComponent(HealthComponent.class);
        float oldMaxHealth = health.maxHealth;
        if (health != null) {
            health.maxHealth = max;
            clientComp.character.saveComponent(health);
        }
        return "Max health changed from " + oldMaxHealth + " to " + max;
    }

    @Command(shortDescription = "Set health regen rate", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String setRegenRate(@Sender EntityRef client, @CommandParam("rate") float rate) {
        ClientComponent clientComp = client.getComponent(ClientComponent.class);
        HealthComponent health = clientComp.character.getComponent(HealthComponent.class);
        float oldRegenRate = health.regenRate;
        if (health != null) {
            health.regenRate = rate;
            clientComp.character.saveComponent(health);
        }
        return "Health regeneration changed from " + oldRegenRate + " to " + rate;
    }

    @Command(shortDescription = "Show your health", requiredPermission = PermissionManager.NO_PERMISSION)
    public String showHealth(@Sender EntityRef client) {
        ClientComponent clientComp = client.getComponent(ClientComponent.class);
        HealthComponent health = clientComp.character.getComponent(HealthComponent.class);
        if (health != null) {
            return "Your health:" + health.currentHealth + " max:" + health.maxHealth + " regen:" + health.regenRate;
        }
        return "I guess you're dead?";
    }


    @Command(shortDescription = "Land without breaking a leg", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String softLanding(@Sender EntityRef client) {
        ClientComponent clientComp = client.getComponent(ClientComponent.class);
        HealthComponent health = clientComp.character.getComponent(HealthComponent.class);
        if (health != null) {
            health.fallingDamageSpeedThreshold = 85f;
            health.excessSpeedDamageMultiplier = 2f;
            clientComp.character.saveComponent(health);

            return "Soft landing mode activated";
        }
        return "";
    }

    @Command(shortDescription = "Restore default collision damage values", runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String restoreCollisionDamage(@Sender EntityRef client) {
        ClientComponent clientComp = client.getComponent(ClientComponent.class);

        Optional<Prefab> prefab = Assets.get(new ResourceUrn("engine:player"), Prefab.class);
        HealthComponent healthDefault = prefab.get().getComponent(HealthComponent.class);
        HealthComponent health = clientComp.character.getComponent(HealthComponent.class);
        if (health != null && healthDefault != null) {
            health.fallingDamageSpeedThreshold = healthDefault.fallingDamageSpeedThreshold;
            health.horizontalDamageSpeedThreshold = healthDefault.horizontalDamageSpeedThreshold;
            health.excessSpeedDamageMultiplier = healthDefault.excessSpeedDamageMultiplier;
            clientComp.character.saveComponent(health);
        }
        return "Normal collision damage values restored";
    }

}
