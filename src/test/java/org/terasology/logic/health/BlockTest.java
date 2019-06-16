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

import com.google.api.client.util.Sets;
import org.junit.Test;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.characters.events.AttackEvent;
import org.terasology.math.geom.Vector3i;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockTest extends ModuleTestingEnvironment {
    private static final Vector3i BLOCK_LOCATION = Vector3i.zero().add(Vector3i.down());

    private WorldProvider worldProvider;
    private Time time;

    private Block testBlock;
    private EntityRef testBlockEntity;

    @Override
    public Set<String> getDependencies() {
        Set<String> modules = Sets.newHashSet();
        modules.add("Health");
        return modules;
    }

    @Override
    public void setup() throws Exception {
        super.setup();

        worldProvider = getHostContext().get(WorldProvider.class);
        time = getHostContext().get(Time.class);

        BlockManager blockManager = getHostContext().get(BlockManager.class);
        testBlock = blockManager.getBlock("health:test");
        forceAndSetBlock(BLOCK_LOCATION, testBlock);

        BlockEntityRegistry blockEntityRegistry = getHostContext().get(BlockEntityRegistry.class);
        testBlockEntity = blockEntityRegistry.getExistingBlockEntityAt(BLOCK_LOCATION);
    }

    private void forceAndSetBlock(Vector3i position, Block material) {
        forceAndWaitForGeneration(position);
        Block result = worldProvider.setBlock(position, material);
    }

    @Test
    public void blockRegenTest() {
        // Attack on block, damage of 1 inflicted
        testBlockEntity.send(new AttackEvent(testBlockEntity, testBlockEntity));

        // Make sure that the attack actually caused damage and started regen
        assertTrue(testBlockEntity.hasComponent(BlockDamagedComponent.class));
        assertTrue(testBlockEntity.hasComponent(RegenComponent.class));

        // Time for regen is 1 sec, 0.2 sec for processing buffer time
        float regenTime = time.getGameTime() + 1 + 0.200f;
        runWhile(()-> time.getGameTime() <= regenTime);

        // On regen, health is fully restored, and BlockDamagedComponent is removed from the block
        assertFalse(testBlockEntity.hasComponent(BlockDamagedComponent.class));
    }

}
