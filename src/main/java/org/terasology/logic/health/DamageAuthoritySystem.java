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

import org.terasology.audio.StaticSound;
import org.terasology.audio.events.PlaySoundEvent;
import org.terasology.audio.events.PlaySoundForOwnerEvent;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.CharacterMovementComponent;
import org.terasology.logic.characters.CharacterSoundComponent;
import org.terasology.logic.characters.CharacterSoundSystem;
import org.terasology.logic.characters.MovementMode;
import org.terasology.logic.characters.events.AttackEvent;
import org.terasology.logic.characters.events.HorizontalCollisionEvent;
import org.terasology.logic.characters.events.VerticalCollisionEvent;
import org.terasology.logic.health.event.ActivateRegenEvent;
import org.terasology.logic.health.event.BeforeDamagedEvent;
import org.terasology.logic.health.event.DamageSoundComponent;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.health.event.DoRestoreEvent;
import org.terasology.logic.health.event.OnDamagedEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.Vector3f;
import org.terasology.registry.In;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;

/**
 * This system reacts to OnDamageEvent events and lowers health on the HealthComponent, and handles
 * horizontal and vertical crashes of entities with HealthComponents.
 *
 * Logic flow for damage:
 * - OnDamageEvent
 * - BeforeDamageEvent
 * - (HealthComponent saved)
 * - OnDamagedEvent
 * - DestroyEvent (if no health)
 *
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class DamageAuthoritySystem extends BaseComponentSystem {

    @In
    private Time time;

    private Random random = new FastRandom();


    /**
     * Override the default behavior for an attack, causing it damage as opposed to just destroying it or doing nothing.
     *
     * @param event Attack event sent on targetEntity.
     * @param targetEntity The entity which is attacked.
     */
    @ReceiveEvent(components = HealthComponent.class, netFilter = RegisterMode.AUTHORITY)
    public void onAttackEntity(AttackEvent event, EntityRef targetEntity) {
        damageEntity(event, targetEntity);
    }

    public static void damageEntity(AttackEvent event, EntityRef targetEntity) {
        int damage = 1;
        Prefab damageType = EngineDamageTypes.PHYSICAL.get();
        // Calculate damage from item
        ItemComponent item = event.getDirectCause().getComponent(ItemComponent.class);
        if (item != null) {
            damage = item.baseDamage;
            if (item.damageType != null) {
                damageType = item.damageType;
            }
        }

        targetEntity.send(new DoDamageEvent(damage, damageType, event.getInstigator(), event.getDirectCause()));
        // consume the event so that the health system can take priority over default engine behavior
        event.consume();
    }

    private void doDamage(EntityRef entity, int damageAmount, Prefab damageType, EntityRef instigator, EntityRef directCause) {
        HealthComponent health = entity.getComponent(HealthComponent.class);
        CharacterMovementComponent characterMovementComponent = entity.getComponent(CharacterMovementComponent.class);
        boolean ghost = false;
        if (characterMovementComponent != null) {
            ghost = (characterMovementComponent.mode == MovementMode.GHOSTING);
        }
        if ((health != null) && !ghost) {
            int cappedDamage = Math.min(health.currentHealth, damageAmount);
            health.currentHealth -= cappedDamage;
            entity.send(new ActivateRegenEvent());
            entity.saveComponent(health);
            entity.send(new OnDamagedEvent(damageAmount, cappedDamage, damageType, instigator));
            if (health.currentHealth == 0 && health.destroyEntityOnNoHealth) {
                entity.send(new DestroyEvent(instigator, directCause, damageType));
            }
        }
    }

    /**
     * Handles DoDamageEvent to inflict damage to entity with HealthComponent.
     *
     * @param event DoDamageEvent causing the damage on the entity.
     * @param entity The entity which is damaged.
     */
    @ReceiveEvent
    public void onDamage(DoDamageEvent event, EntityRef entity) {
        checkDamage(entity, event.getAmount(), event.getDamageType(), event.getInstigator(), event.getDirectCause());
    }

    private void checkDamage(EntityRef entity, int amount, Prefab damageType, EntityRef instigator, EntityRef directCause) {
        BeforeDamagedEvent beforeDamage = entity.send(new BeforeDamagedEvent(amount, damageType, instigator, directCause));
        if (!beforeDamage.isConsumed()) {
            int damageAmount = TeraMath.floorToInt(beforeDamage.getResultValue());
            if (damageAmount >= 0) {
                doDamage(entity, damageAmount, damageType, instigator, directCause);
            } else {
                entity.send(new DoRestoreEvent(-damageAmount, instigator));
            }
        }
    }

    /**
     * Handles damage sound on inflicting damage to entity.
     *
     * @param event OnDamagedEvent triggered when entity is damaged.
     * @param entity Entity which is damaged.
     * @param characterSounds Component having sound settings.
     */
    @ReceiveEvent
    public void onDamaged(OnDamagedEvent event, EntityRef entity, CharacterSoundComponent characterSounds)  {
        if (characterSounds.lastSoundTime + CharacterSoundSystem.MIN_TIME < time.getGameTimeInMs()) {

            // play the sound of damage hitting the character for everyone
            DamageSoundComponent damageSounds = event.getType().getComponent(DamageSoundComponent.class);
            if (damageSounds != null && !damageSounds.sounds.isEmpty()) {
                StaticSound sound = random.nextItem(damageSounds.sounds);
                if (sound != null) {
                    entity.send(new PlaySoundEvent(sound, 1f));
                }
            }

            // play the sound of a client's character being damaged to the client
            if (!characterSounds.damageSounds.isEmpty()) {
                StaticSound sound = random.nextItem(characterSounds.damageSounds);
                if (sound != null) {
                    entity.send(new PlaySoundForOwnerEvent(sound, characterSounds.damageVolume));
                }
            }

            characterSounds.lastSoundTime = time.getGameTimeInMs();
            entity.saveComponent(characterSounds);

        }
    }

    /**
     * Causes damage to entity when fallingDamageSpeedThreshold is breached.
     *
     * @param event VerticalCollisionEvent sent when falling speed threshold is crossed.
     * @param entity The entity which is damaged due to falling.
     */
    @ReceiveEvent
    public void onLand(VerticalCollisionEvent event, EntityRef entity, HealthComponent health) {
        float speed = Math.abs(event.getVelocity().y);

        highSpeedDamage(speed, entity, health.fallingDamageSpeedThreshold, health.excessSpeedDamageMultiplier);
    }

    /**
     * Inflicts damage to entity if horizontalDamageSpeedThreshold is breached.
     *
     * @param event HorizontalCollisionEvent sent when "falling horizontally".
     * @param entity Entity which is damaged on "horizontal fall".
     */
    @ReceiveEvent
    public void onCrash(HorizontalCollisionEvent event, EntityRef entity, HealthComponent health) {
        Vector3f vel = new Vector3f(event.getVelocity());
        vel.y = 0;
        float speed = vel.length();

        highSpeedDamage(speed, entity, health.horizontalDamageSpeedThreshold, health.excessSpeedDamageMultiplier);
    }

    private void highSpeedDamage(float speed, EntityRef entity, float threshold, float damageMultiplier) {
        if (speed > threshold) {
            int damage = (int) ((speed - threshold) * damageMultiplier);
            if (damage > 0) {
                checkDamage(entity, damage, EngineDamageTypes.PHYSICAL.get(), EntityRef.NULL, EntityRef.NULL);
            }
        }
    }

    /**
     * Plays landing sound on crashing horizontally.
     *
     * @param event HorizontalCollisionEvent sent when "falling horizontally".
     * @param entity Entity which is damaged on "horizontal fall".
     * @param characterSounds For getting the sound to be played on crash.
     * @param healthComponent To play sound only when threshold speed is crossed.
     */
    @ReceiveEvent
    public void onCrash(HorizontalCollisionEvent event, EntityRef entity, CharacterSoundComponent characterSounds, HealthComponent healthComponent) {
        Vector3f horizVelocity = new Vector3f(event.getVelocity());
        horizVelocity.y = 0;
        float velocity = horizVelocity.length();

        if (velocity > healthComponent.horizontalDamageSpeedThreshold) {
            if (characterSounds.lastSoundTime + CharacterSoundSystem.MIN_TIME < time.getGameTimeInMs()) {
                StaticSound sound = random.nextItem(characterSounds.landingSounds);
                if (sound != null) {
                    entity.send(new PlaySoundEvent(sound, characterSounds.landingVolume));
                    characterSounds.lastSoundTime = time.getGameTimeInMs();
                    entity.saveComponent(characterSounds);
                }
            }
        }
    }
}
