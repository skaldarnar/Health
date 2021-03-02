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

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.characters.events.AttackEvent;
import org.terasology.moduletestingenvironment.MTEExtension;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MTEExtension.class)
@Dependencies({"Health"})
@Disabled("The test has some weird timing issues which will sporadically fail it. (see #70)")
public class BlockTest {
    private static final Vector3ic BLOCK_LOCATION = new Vector3i(0, 0, 0).add(0, -1, 0);

    private static final long BUFFER = 200; // 200 ms buffer time

    @In
    protected WorldProvider worldProvider;
    @In
    protected Time time;
    @In
    protected BlockManager blockManager;
    @In
    protected BlockEntityRegistry blockEntityRegistry;
    @In
    protected ModuleTestingHelper helper;

    @Test
    public void blockRegenTest() {
        Block testBlock = blockManager.getBlock("health:test");

        helper.forceAndWaitForGeneration(BLOCK_LOCATION);
        worldProvider.setBlock(BLOCK_LOCATION, testBlock);

        EntityRef testBlockEntity = blockEntityRegistry.getExistingBlockEntityAt(BLOCK_LOCATION);

        // Attack on block, damage of 1 inflicted
        float currentTime = time.getGameTime();
        testBlockEntity.send(new AttackEvent(testBlockEntity, testBlockEntity));

        // Make sure that the attack actually caused damage and started regen
        assertFalse(helper.runUntil(BUFFER, () -> testBlockEntity.hasComponent(BlockDamagedComponent.class)), "time out");
        assertTrue(testBlockEntity.hasComponent(BlockDamagedComponent.class));

        // Regen effects starts delayed after 1 second by default, so let's wait
        assertFalse(helper.runUntil(1000 + BUFFER, () -> testBlockEntity.hasComponent(RegenComponent.class)), "time out");
        assertTrue(testBlockEntity.hasComponent(RegenComponent.class));

        // Time for regen is 1 sec, 0.2 sec for processing buffer time
        assertFalse(helper.runUntil(3000 + BUFFER, () -> !testBlockEntity.hasComponent(BlockDamagedComponent.class)), "time out");

        // On regen, health is fully restored, and BlockDamagedComponent is removed from the block
        assertFalse(testBlockEntity.hasComponent(BlockDamagedComponent.class));
    }
}
