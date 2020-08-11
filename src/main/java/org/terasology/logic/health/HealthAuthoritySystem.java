// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.logic.health;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.health.event.ChangeMaxHealthEvent;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.health.event.MaxHealthChangedEvent;
import org.terasology.nui.widgets.UIIconBar;
import org.terasology.registry.In;
import org.terasology.rendering.nui.NUIManager;

@RegisterSystem(value = RegisterMode.AUTHORITY)
public class HealthAuthoritySystem extends BaseComponentSystem {
    @In
    NUIManager nuiManager;
    @In
    PrefabManager prefabManager;

    /**
     * Sends out an immutable notification event when maxHealth of a character is changed.
     */
    @ReceiveEvent(priority = EventPriority.PRIORITY_TRIVIAL)
    public void changeMaxHealth(ChangeMaxHealthEvent event, EntityRef player, HealthComponent health) {
        int oldMaxHealth = health.maxHealth;
        health.maxHealth = (int) event.getResultValue();
        Prefab maxHealthReductionDamagePrefab = prefabManager.getPrefab("Health:maxHealthReductionDamage");
        player.send(new DoDamageEvent(Math.max(health.currentHealth - health.maxHealth, 0),
                maxHealthReductionDamagePrefab));
        player.send(new MaxHealthChangedEvent(oldMaxHealth, health.maxHealth));
        player.saveComponent(health);
    }

    /**
     * Reacts to the {@link MaxHealthChangedEvent} notification event. Is responsible for the change in maximum number
     * of icons in the Health Bar UI.
     */
    @ReceiveEvent
    public void onMaxHealthChanged(MaxHealthChangedEvent event, EntityRef player) {
        UIIconBar healthBar = nuiManager.getHUD().find("healthBar", UIIconBar.class);
        healthBar.setMaxIcons(event.getNewValue() / 10);
    }
}
