// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.logic.health;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.joml.Math;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.RenderSystem;
import org.terasology.registry.In;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureRegionAsset;
import org.terasology.rendering.world.selection.BlockSelectionRenderer;
import org.terasology.utilities.Assets;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.regions.BlockRegionComponent;

import java.util.Optional;

/**
 * This system renders damage damaged blocks using the BlockSelectionRenderer.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class BlockDamageRenderer extends BaseComponentSystem implements RenderSystem {

    private BlockSelectionRenderer blockSelectionRenderer;

    @In
    private EntityManager entityManager;

    @Override
    public void renderOverlay() {
        if (blockSelectionRenderer == null) {
            Texture texture = Assets.getTextureRegion("CoreAssets:blockDamageEffects#1").get().getTexture();
            blockSelectionRenderer = new BlockSelectionRenderer(texture);
        }
        // group the entities into what texture they will use so that there is less recreating meshes (changing a
        // texture region on the BlockSelectionRenderer
        // will recreate the mesh to use the different UV coordinates).  Also this allows
        Multimap<Integer, Vector3i> groupedEntitiesByEffect = ArrayListMultimap.create();

        for (EntityRef entity : entityManager.getEntitiesWith(HealthComponent.class, BlockComponent.class)) {
            HealthComponent health = entity.getComponent(HealthComponent.class);
            if (health.currentHealth == health.maxHealth) {
                continue;
            }
            BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
            groupedEntitiesByEffect.put(getDamageEffectsNumber(health), blockComponent.getPosition(new Vector3i()));
        }
        for (EntityRef entity : entityManager.getEntitiesWith(HealthComponent.class, BlockRegionComponent.class)) {
            HealthComponent health = entity.getComponent(HealthComponent.class);
            if (health.currentHealth == health.maxHealth) {
                continue;
            }
            BlockRegionComponent blockRegion = entity.getComponent(BlockRegionComponent.class);
            for (Vector3ic blockPos : blockRegion.region) {
                groupedEntitiesByEffect.put(getDamageEffectsNumber(health), new Vector3i(blockPos));
            }
        }

        // we know that the texture will be the same for each block effect,  just different UV coordinates.
        // Bind the texture already
        blockSelectionRenderer.beginRenderOverlay();

        for (Integer effectsNumber : groupedEntitiesByEffect.keySet()) {
            Optional<TextureRegionAsset> texture =
                    Assets.getTextureRegion("CoreAssets:blockDamageEffects#" + effectsNumber);
            if (texture.isPresent()) {
                blockSelectionRenderer.setEffectsTexture(texture.get());
                for (Vector3i position : groupedEntitiesByEffect.get(effectsNumber)) {
                    blockSelectionRenderer.renderMark(position);
                }
            }
        }

        blockSelectionRenderer.endRenderOverlay();
    }


    /**
     * Compute the damage effect number as linear mapping from damage percentage to the range [0..10].
     * <p>
     * A value of 0 denotes no damage, while 10 denotes a current health of 0.
     *
     * @return the effect number in [0..10] linear to damage percentage
     */
    int getDamageEffectsNumber(HealthComponent health) {
        Preconditions.checkArgument(health.currentHealth >= 0);
        Preconditions.checkArgument(health.maxHealth > 0);

        float damagePercentage = 1f - Math.clamp(0f, 1f, (float) health.currentHealth / health.maxHealth);
        return Math.round(damagePercentage * 10);
    }


    @Override
    public void renderShadows() {
    }

    @Override
    public void renderOpaque() {
    }

    @Override
    public void renderAlphaBlend() {
    }
}
