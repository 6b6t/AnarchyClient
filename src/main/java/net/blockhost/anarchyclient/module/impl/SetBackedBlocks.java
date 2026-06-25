package net.blockhost.anarchyclient.module.impl;

import net.minecraft.world.level.block.Block;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SetBackedBlocks {

    private SetBackedBlocks() {
    }

    static Set<Block> ids(final List<Block> blocks) {
        return new LinkedHashSet<>(blocks);
    }
}
