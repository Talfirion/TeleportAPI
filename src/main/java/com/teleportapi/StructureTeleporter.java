package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class StructureTeleporter {

    // Класс для хранения информации о блоке
    public static class BlockData {
        public BlockPos relativePos; // Позиция относительно начальной точки
        public BlockState blockState; // Состояние блока (тип блока + его данные)
        public CompoundTag nbt; // Данные BlockEntity (сундуки и т.д.)

        public BlockData(BlockPos relativePos, BlockState blockState, CompoundTag nbt) {
            this.relativePos = relativePos;
            this.blockState = blockState;
            this.nbt = nbt;
        }
    }

    // Метод для копирования структуры в память
    public static List<BlockData> copyStructure(Selection selection) {
        if (!selection.isComplete()) {
            return null;
        }

        List<BlockData> blocks = new ArrayList<>();

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();
        Level world = selection.getWorld();

        // Проходим по всем блокам в выделенной области
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    // Получаем NBT данные, если есть (для сундуков и т.д.)
                    CompoundTag nbt = null;
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity != null) {
                        nbt = blockEntity.saveWithFullMetadata();
                        // Удаляем координаты из NBT, чтобы они не конфликтовали при вставке
                        nbt.remove("x");
                        nbt.remove("y");
                        nbt.remove("z");
                    }

                    // Вычисляем относительную позицию (относительно минимальной точки)
                    BlockPos relativePos = pos.subtract(min);

                    // Сохраняем блок
                    blocks.add(new BlockData(relativePos, state, nbt));
                }
            }
        }

        TeleportAPI.LOGGER.info("Скопировано блоков: " + blocks.size());
        return blocks;
    }

    // Метод для вставки структуры в новое место
    public static void pasteStructure(List<BlockData> blocks, BlockPos targetPos, Level world) {
        if (blocks == null || blocks.isEmpty()) {
            TeleportAPI.LOGGER.warn("Нет блоков для вставки!");
            return;
        }

        // Вставляем каждый блок
        for (BlockData blockData : blocks) {
            // Вычисляем абсолютную позицию (целевая позиция + относительная)
            BlockPos absolutePos = targetPos.offset(blockData.relativePos);

            // Устанавливаем блок
            world.setBlock(absolutePos, blockData.blockState, 3);

            // Восстанавливаем NBT данные (если есть)
            if (blockData.nbt != null) {
                BlockEntity blockEntity = world.getBlockEntity(absolutePos);
                if (blockEntity != null) {
                    // Создаем копию NBT и обновляем координаты
                    CompoundTag tag = blockData.nbt.copy();
                    tag.putInt("x", absolutePos.getX());
                    tag.putInt("y", absolutePos.getY());
                    tag.putInt("z", absolutePos.getZ());

                    blockEntity.load(tag);
                }
            }
        }

        TeleportAPI.LOGGER.info("Вставлено блоков: " + blocks.size() + " в позицию " + targetPos);
    }

    // Метод для телепортации структуры (удалить из старого места и вставить в
    // новое)
    public static void teleportStructure(Selection selection, BlockPos targetPos) {
        if (!selection.isComplete()) {
            TeleportAPI.LOGGER.warn("Выделение не завершено!");
            return;
        }

        Level world = selection.getWorld();

        // 1. Копируем структуру
        List<BlockData> blocks = copyStructure(selection);

        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        // 2. Удаляем блоки из старого места (заменяем на воздух)
        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    // Устанавливаем воздух (удаляем блок)
                    world.removeBlock(pos, false);
                }
            }
        }

        // 3. Вставляем в новое место
        pasteStructure(blocks, targetPos, world);

        TeleportAPI.LOGGER.info("Структура телепортирована в " + targetPos);
    }

    /**
     * Телепортировать структуру, определенную двумя углами.
     */
    public static void teleportByCorners(Level world, BlockPos p1, BlockPos p2, BlockPos targetPos) {
        Selection selection = new Selection();
        selection.setWorld(world);
        selection.setFromCorners(p1, p2);
        teleportStructure(selection, targetPos);
    }

    /**
     * Телепортировать структуру, определенную 6 точками.
     */
    public static void teleportBySixPoints(Level world, BlockPos[] points, BlockPos targetPos) {
        Selection selection = new Selection();
        selection.setWorld(world);
        selection.setFromSixPoints(points);
        teleportStructure(selection, targetPos);
    }
}
