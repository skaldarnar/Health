// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.logic.health;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.health.event.ChangeMaxHealthEvent;
import org.terasology.logic.health.event.OnMaxHealthChangedEvent;
import org.terasology.registry.In;
import org.terasology.rendering.nui.NUIManager;
import org.terasology.rendering.nui.widgets.UIIconBar;

@RegisterSystem(value = RegisterMode.AUTHORITY)
public class HealthAuthoritySystem extends BaseComponentSystem {
    @In
    NUIManager nuiManager;

    @ReceiveEvent(priority = EventPriority.PRIORITY_TRIVIAL)
    public void changeMaxHealth(ChangeMaxHealthEvent event, EntityRef player, HealthComponent health) {
        player.send(new OnMaxHealthChangedEvent(health.maxHealth, (int) event.getResultValue()));
        health.maxHealth = (int) event.getResultValue();
        player.saveComponent(health);
    }

    @ReceiveEvent
    public void onMaxHealthChanged(OnMaxHealthChangedEvent event, EntityRef player) {
        UIIconBar healthBar = nuiManager.getHUD().find("healthBar", UIIconBar.class);
        healthBar.setMaxIcons(event.getNewValue() / 10);
    }
}
