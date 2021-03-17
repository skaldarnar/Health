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

import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.terasology.engine.audio.AudioManager;
import org.terasology.engine.audio.StaticSound;
import org.terasology.engine.audio.events.PlaySoundEvent;
import org.terasology.engine.entitySystem.entity.EntityBuilder;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.events.AttackEvent;
import org.terasology.logic.health.HealthComponent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.particles.components.ParticleDataSpriteComponent;
import org.terasology.engine.particles.components.generators.TextureOffsetGeneratorComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.assets.texture.Texture;
import org.terasology.engine.utilities.Assets;
import org.terasology.engine.utilities.random.FastRandom;
import org.terasology.engine.utilities.random.Random;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockAppearance;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.BlockPart;
import org.terasology.engine.world.block.entity.damage.BlockDamageModifierComponent;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.engine.world.block.regions.ActAsBlockComponent;
import org.terasology.engine.world.block.sounds.BlockSounds;
import org.terasology.engine.world.block.tiles.WorldAtlas;
import org.terasology.logic.health.event.BeforeDamagedEvent;
import org.terasology.logic.health.event.OnDamagedEvent;
import org.terasology.logic.health.event.OnFullyHealedEvent;
import org.terasology.math.TeraMath;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This system is responsible for giving blocks health when they are attacked and
 * damaging them instead of destroying them.
 */
@RegisterSystem
public class BlockDamageAuthoritySystem extends BaseComponentSystem {
    private static final float BLOCK_REGEN_SECONDS = 4.0f;

    @In
    private EntityManager entityManager;

    @In
    private AudioManager audioManager;

    @In
    private WorldAtlas worldAtlas;

    @In
    private BlockManager blockManager;

    private Random random = new FastRandom();

    /** Consumes damage event if block is indestructible. */
    @ReceiveEvent
    public void beforeDamaged(BeforeDamagedEvent event, EntityRef blockEntity, BlockComponent blockComp) {
        if (!blockComp.getBlock().isDestructible()) {
            event.consume();
        }
    }

    /** Consumes damage event if entity acting as block is indestructible. */
    @ReceiveEvent
    public void beforeDamaged(BeforeDamagedEvent event, EntityRef blockEntity, ActAsBlockComponent blockComp) {
        if (blockComp.block != null && !blockComp.block.getArchetypeBlock().isDestructible()) {
            event.consume();
        }
    }

    /**
     * Removes the marker component when block is fully healed.
     * @param event Event sent when block is fully healed
     * @param entity Block entity
     */
    @ReceiveEvent(components = {BlockDamagedComponent.class})
    public void onRepaired(OnFullyHealedEvent event, EntityRef entity) {
        entity.removeComponent(BlockDamagedComponent.class);
    }

    /** Adds marker component to block which is damaged. */
    @ReceiveEvent
    public void onDamaged(OnDamagedEvent event, EntityRef entity, BlockComponent blockComponent, LocationComponent locComp) {
        onDamagedCommon(event, blockComponent.getBlock().getBlockFamily(), locComp.getWorldPosition(new Vector3f()), entity);
        if (!entity.hasComponent(BlockDamagedComponent.class)) {
            entity.addComponent(new BlockDamagedComponent());
        }
    }

    @ReceiveEvent
    public void onDamaged(OnDamagedEvent event, EntityRef entity, ActAsBlockComponent blockComponent, LocationComponent locComp) {
        if (blockComponent.block != null) {
            onDamagedCommon(event, blockComponent.block, locComp.getWorldPosition(new Vector3f()), entity);
        }
    }

    private void onDamagedCommon(OnDamagedEvent event, BlockFamily blockFamily, Vector3fc location, EntityRef entityRef) {
        BlockDamageModifierComponent blockDamageSettings = event.getType().getComponent(BlockDamageModifierComponent.class);
        boolean skipDamageEffects = false;
        if (blockDamageSettings != null) {
            skipDamageEffects = blockDamageSettings.skipPerBlockEffects;
        }
        if (!skipDamageEffects) {
            onPlayBlockDamageCommon(blockFamily, location, entityRef);
        }
    }

    /** Calls helper function to create block particle effect and plays damage sound. */
    private void onPlayBlockDamageCommon(BlockFamily family, Vector3fc location, EntityRef entityRef) {
        createBlockParticleEffect(family, location);

        BlockSounds sounds = family.getArchetypeBlock().getSounds();
        if (!sounds.getDigSounds().isEmpty()) {
            StaticSound sound = random.nextItem(sounds.getDigSounds());
            entityRef.send(new PlaySoundEvent(sound, 1f));
        }
    }

    /**
     * Creates a new entity for the block damage particle effect.
     *
     * If the terrain texture of the damaged block is available, the particles will have the block texture. Otherwise,
     * the default sprite (smoke) is used.
     *
     * @param family the {@link BlockFamily} of the damaged block
     * @param location the location of the damaged block
     */
    private void createBlockParticleEffect(BlockFamily family, Vector3fc location) {
        EntityBuilder builder = entityManager.newBuilder("CoreAssets:defaultBlockParticles");
        builder.getComponent(LocationComponent.class).setWorldPosition(location);

        Optional<Texture> terrainTexture = Assets.getTexture("engine:terrain");
        if (terrainTexture.isPresent() && terrainTexture.get().isLoaded()) {
            final BlockAppearance blockAppearance = family.getArchetypeBlock().getPrimaryAppearance();

            final float relativeTileSize = worldAtlas.getRelativeTileSize();
            final float particleScale = 0.25f;

            final float spriteSize = relativeTileSize * particleScale;

            ParticleDataSpriteComponent spriteComponent = builder.getComponent(ParticleDataSpriteComponent.class);
            spriteComponent.texture = terrainTexture.get();
            spriteComponent.textureSize.set(spriteSize, spriteSize);

            final List<org.joml.Vector2f> offsets = computeOffsets(blockAppearance, particleScale);

            TextureOffsetGeneratorComponent textureOffsetGeneratorComponent = builder.getComponent(TextureOffsetGeneratorComponent.class);
            textureOffsetGeneratorComponent.validOffsets.addAll(offsets);
        }

        builder.build();
    }

    /**
     * Computes n random offset values for each block part texture.
     *
     * @param blockAppearance the block appearance information to generate offsets from
     * @param scale the scale of the texture area (should be in 0 < scale <= 1.0)
     *
     * @return a list of random offsets sampled from all block parts
     */
    private List<org.joml.Vector2f> computeOffsets(BlockAppearance blockAppearance, float scale) {
        final float relativeTileSize = worldAtlas.getRelativeTileSize();
        final int absoluteTileSize = worldAtlas.getTileSize();
        final float pixelSize = relativeTileSize / absoluteTileSize;
        final int spriteWidth = TeraMath.ceilToInt(scale * absoluteTileSize);

        final Stream<Vector2fc> baseOffsets = Arrays.stream(BlockPart.sideValues()).map(blockAppearance::getTextureAtlasPos);

        return baseOffsets.flatMap(baseOffset ->
                    IntStream.range(0, 8).boxed().map(i ->
                        new org.joml.Vector2f(baseOffset).add(random.nextInt(absoluteTileSize - spriteWidth) * pixelSize, random.nextInt(absoluteTileSize - spriteWidth) * pixelSize)
                    )
                ).collect(Collectors.toList());
    }

    @ReceiveEvent(netFilter = RegisterMode.AUTHORITY)
    public void beforeDamage(BeforeDamagedEvent event, EntityRef entity, BlockComponent blockComp) {
        beforeDamageCommon(event, blockComp.getBlock());
    }

    @ReceiveEvent(netFilter = RegisterMode.AUTHORITY)
    public void beforeDamage(BeforeDamagedEvent event, EntityRef entity, ActAsBlockComponent blockComp) {
        if (blockComp.block != null) {
            beforeDamageCommon(event, blockComp.block.getArchetypeBlock());
        }
    }

    private void beforeDamageCommon(BeforeDamagedEvent event, Block block) {
        if (event.getDamageType() != null) {
            BlockDamageModifierComponent blockDamage = event.getDamageType().getComponent(BlockDamageModifierComponent.class);
            if (blockDamage != null) {
                BlockFamily blockFamily = block.getBlockFamily();
                for (String category : blockFamily.getCategories()) {
                    if (blockDamage.materialDamageMultiplier.containsKey(category)) {
                        event.multiply(blockDamage.materialDamageMultiplier.get(category));
                    }
                }
            }
        }
    }

    /** Causes damage to block without health component, leads to adding health component to the block. */
    @ReceiveEvent(netFilter = RegisterMode.AUTHORITY)
    public void onAttackHealthlessBlock(AttackEvent event, EntityRef targetEntity, BlockComponent blockComponent) {
        if (!targetEntity.hasComponent(HealthComponent.class)) {
            DamageAuthoritySystem.damageEntity(event, targetEntity);
        }
    }

    @ReceiveEvent(netFilter = RegisterMode.AUTHORITY)
    public void onAttackHealthlessActAsBlock(AttackEvent event, EntityRef targetEntity, ActAsBlockComponent actAsBlockComponent) {
        if (!targetEntity.hasComponent(HealthComponent.class)) {
            DamageAuthoritySystem.damageEntity(event, targetEntity);
        }
    }

    /** Adds health component to blocks when damaged. */
    @ReceiveEvent
    public void beforeDamagedEnsureHealthPresent(BeforeDamagedEvent event, EntityRef blockEntity, BlockComponent blockComponent) {
        if (!blockEntity.hasComponent(HealthComponent.class)) {
            Block type = blockComponent.getBlock();
            if (type.isDestructible()) {
                HealthComponent healthComponent = new HealthComponent();
                healthComponent.maxHealth = type.getHardness();
                healthComponent.currentHealth = type.getHardness();
                healthComponent.destroyEntityOnNoHealth = true;
                healthComponent.regenRate = type.getHardness() / BLOCK_REGEN_SECONDS;
                healthComponent.waitBeforeRegen = 1f;

                blockEntity.addComponent(healthComponent);
            }
        }
    }
}
