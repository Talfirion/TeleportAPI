package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс для хранения информации о выделенной области.
 * Область определяется через 6 точек в пространстве - по одной для каждой грани
 * параллелепипеда.
 * Для axis-aligned box каждая точка определяет положение грани вдоль
 * соответствующей оси.
 */
public class Selection {
    // Хранилище для точек граней параллелепипеда
    private Map<FaceType, BlockPos> facePoints = new HashMap<>();

    // Мир, в котором сделано выделение
    private Level world;

    /**
     * Установить точку для определенной грани параллелепипеда
     * 
     * @param faceType тип грани (X_MIN, X_MAX, Y_MIN, Y_MAX, Z_MIN, Z_MAX)
     * @param point    точка в пространстве, через которую проходит грань
     */
    public void setFacePoint(FaceType faceType, BlockPos point) {
        facePoints.put(faceType, point);
        validateCoordinates();
    }

    /**
     * Получить точку для определенной грани
     */
    public BlockPos getFacePoint(FaceType faceType) {
        return facePoints.get(faceType);
    }

    /**
     * Удалить точку для определенной грани
     */
    public void removeFacePoint(FaceType faceType) {
        facePoints.remove(faceType);
    }

    // Геттеры для координат граней (для совместимости)

    public Integer getXMin() {
        BlockPos point = facePoints.get(FaceType.X_MIN);
        return point != null ? point.getX() : null;
    }

    public Integer getXMax() {
        BlockPos point = facePoints.get(FaceType.X_MAX);
        return point != null ? point.getX() : null;
    }

    public Integer getYMin() {
        BlockPos point = facePoints.get(FaceType.Y_MIN);
        return point != null ? point.getY() : null;
    }

    public Integer getYMax() {
        BlockPos point = facePoints.get(FaceType.Y_MAX);
        return point != null ? point.getY() : null;
    }

    public Integer getZMin() {
        BlockPos point = facePoints.get(FaceType.Z_MIN);
        return point != null ? point.getZ() : null;
    }

    public Integer getZMax() {
        BlockPos point = facePoints.get(FaceType.Z_MAX);
        return point != null ? point.getZ() : null;
    }

    // Сеттеры для удобства (создают точки с нулевыми неиспользуемыми координатами)

    public void setXMin(int x) {
        setFacePoint(FaceType.X_MIN, new BlockPos(x, 0, 0));
    }

    public void setXMax(int x) {
        setFacePoint(FaceType.X_MAX, new BlockPos(x, 0, 0));
    }

    public void setYMin(int y) {
        setFacePoint(FaceType.Y_MIN, new BlockPos(0, y, 0));
    }

    public void setYMax(int y) {
        setFacePoint(FaceType.Y_MAX, new BlockPos(0, y, 0));
    }

    public void setZMin(int z) {
        setFacePoint(FaceType.Z_MIN, new BlockPos(0, 0, z));
    }

    public void setZMax(int z) {
        setFacePoint(FaceType.Z_MAX, new BlockPos(0, 0, z));
    }

    public Level getWorld() {
        return world;
    }

    public void setWorld(Level world) {
        this.world = world;
    }

    // Валидация координат - проверка что min <= max для каждой оси
    private void validateCoordinates() {
        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        if (xMin != null && xMax != null && xMin > xMax) {
            TeleportAPI.LOGGER.warn("Внимание: xMin (" + xMin + ") > xMax (" + xMax + ")!");
        }
        if (yMin != null && yMax != null && yMin > yMax) {
            TeleportAPI.LOGGER.warn("Внимание: yMin (" + yMin + ") > yMax (" + yMax + ")!");
        }
        if (zMin != null && zMax != null && zMin > zMax) {
            TeleportAPI.LOGGER.warn("Внимание: zMin (" + zMin + ") > zMax (" + zMax + ")!");
        }
    }

    // Проверка, что все стороны установлены
    public boolean isComplete() {
        return facePoints.size() == 6 && world != null;
    }

    // Получить минимальную точку (левый нижний передний угол)
    public BlockPos getMin() {
        if (!isComplete())
            return null;

        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        return new BlockPos(
                Math.min(xMin, xMax),
                Math.min(yMin, yMax),
                Math.min(zMin, zMax));
    }

    // Получить максимальную точку (правый верхний задний угол)
    public BlockPos getMax() {
        if (!isComplete())
            return null;

        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        return new BlockPos(
                Math.max(xMin, xMax),
                Math.max(yMin, yMax),
                Math.max(zMin, zMax));
    }

    // Получить размер выделенной области (объём в блоках)
    public int getVolume() {
        if (!isComplete())
            return 0;

        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        int sizeX = Math.abs(xMax - xMin) + 1;
        int sizeY = Math.abs(yMax - yMin) + 1;
        int sizeZ = Math.abs(zMax - zMin) + 1;

        return sizeX * sizeY * sizeZ;
    }

    // Метод для вывода информации о выделении
    public String getInfo() {
        if (!isComplete()) {
            return "Выделение не завершено. Установлено граней: " + facePoints.size() + "/6";
        }

        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        StringBuilder info = new StringBuilder();
        info.append(String.format("Выделение: X[%d..%d] Y[%d..%d] Z[%d..%d], Объём: %d блоков\n",
                xMin, xMax, yMin, yMax, zMin, zMax, getVolume()));

        info.append("Точки граней:\n");
        for (FaceType faceType : FaceType.values()) {
            BlockPos point = facePoints.get(faceType);
            if (point != null) {
                info.append(String.format("  %s: %s\n", faceType.getDescription(), point.toShortString()));
            }
        }

        return info.toString();
    }

    // Метод для сброса всех сторон
    public void reset() {
        facePoints.clear();
        world = null;
    }

    /**
     * Получить количество установленных граней
     */
    public int getSetFacesCount() {
        return facePoints.size();
    }

    /**
     * Установить выделение из произвольных точек в пространстве.
     * Метод автоматически вычисляет минимальные и максимальные координаты,
     * и формирует axis-aligned bounding box (параллелепипед).
     * 
     * @param points массив точек (обычно 6 точек, но может быть любое количество)
     * @throws IllegalArgumentException если массив пустой или null
     */
    public void setFromPoints(BlockPos... points) {
        if (points == null || points.length == 0) {
            throw new IllegalArgumentException("Массив точек не может быть пустым!");
        }

        // Инициализируем min/max первой точкой
        int minX = points[0].getX();
        int maxX = points[0].getX();
        int minY = points[0].getY();
        int maxY = points[0].getY();
        int minZ = points[0].getZ();
        int maxZ = points[0].getZ();

        // Проходим по всем остальным точкам и находим минимумы/максимумы
        for (int i = 1; i < points.length; i++) {
            BlockPos point = points[i];

            if (point.getX() < minX)
                minX = point.getX();
            if (point.getX() > maxX)
                maxX = point.getX();

            if (point.getY() < minY)
                minY = point.getY();
            if (point.getY() > maxY)
                maxY = point.getY();

            if (point.getZ() < minZ)
                minZ = point.getZ();
            if (point.getZ() > maxZ)
                maxZ = point.getZ();
        }

        // Устанавливаем грани найденными координатами
        // Используем сами точки для хранения полной информации
        setFacePoint(FaceType.X_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.X_MAX, new BlockPos(maxX, maxY, maxZ));
        setFacePoint(FaceType.Y_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.Y_MAX, new BlockPos(maxX, maxY, maxZ));
        setFacePoint(FaceType.Z_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.Z_MAX, new BlockPos(maxX, maxY, maxZ));

        TeleportAPI.LOGGER.info(String.format(
                "Построен параллелепипед из %d точек: X[%d..%d] Y[%d..%d] Z[%d..%d]",
                points.length, minX, maxX, minY, maxY, minZ, maxZ));
    }

    /**
     * Установить выделение по двум точкам (углам).
     * Это классический способ выделения региона (как в WorldEdit).
     * 
     * @param p1 Первая точка
     * @param p2 Вторая точка
     */
    public void setFromCorners(BlockPos p1, BlockPos p2) {
        if (p1 == null || p2 == null) {
            throw new IllegalArgumentException("Точки не могут быть null!");
        }

        int minX = Math.min(p1.getX(), p2.getX());
        int maxX = Math.max(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int maxY = Math.max(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        setFacePoint(FaceType.X_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.X_MAX, new BlockPos(maxX, maxY, maxZ));
        setFacePoint(FaceType.Y_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.Y_MAX, new BlockPos(maxX, maxY, maxZ));
        setFacePoint(FaceType.Z_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.Z_MAX, new BlockPos(maxX, maxY, maxZ));
    }

    /**
     * Установить выделение по 6 точкам.
     * Требуется ровно 6 точек.
     * 
     * @param points Массив из 6 точек
     */
    public void setFromSixPoints(BlockPos... points) {
        if (points == null || points.length != 6) {
            throw new IllegalArgumentException("Требуется ровно 6 точек!");
        }
        setFromPoints(points);
    }

    /**
     * Установить выделение из произвольных точек в пространстве с указанием мира.
     * 
     * @param world  мир, в котором находится выделение
     * @param points массив точек
     */
    public void setFromPoints(Level world, BlockPos... points) {
        this.world = world;
        setFromPoints(points);
    }
}
