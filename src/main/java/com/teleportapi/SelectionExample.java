package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Пример использования новой системы выделения с 6 точками граней.
 * 
 * Этот класс демонстрирует два способа использования Selection:
 * 1. Старый способ (обратная совместимость) - через scalar coordinates
 * 2. Новый способ - через BlockPos точки для каждой грани
 */
public class SelectionExample {

    /**
     * Пример 1: Использование старого API (обратная совместимость)
     * Работает так же, как и раньше
     */
    public static Selection createSelectionOldWay(Level world) {
        Selection selection = new Selection();
        selection.setWorld(world);

        // Устанавливаем координаты граней как раньше
        selection.setXMin(10);
        selection.setXMax(20);
        selection.setYMin(64);
        selection.setYMax(80);
        selection.setZMin(30);
        selection.setZMax(50);

        return selection;
    }

    /**
     * Пример 2: Использование нового API с точками граней
     * Теперь каждая грань определяется точкой в пространстве (BlockPos)
     */
    public static Selection createSelectionNewWay(Level world) {
        Selection selection = new Selection();
        selection.setWorld(world);

        // Устанавливаем точки для каждой грани
        // Для axis-aligned box учитывается только соответствующая координата:
        // - Для X-граней: x-координата точки
        // - Для Y-граней: y-координата точки
        // - Для Z-граней: z-координата точки

        // Левая грань (x = 10) - проходит через точку (10, 65, 35)
        selection.setFacePoint(FaceType.X_MIN, new BlockPos(10, 65, 35));

        // Правая грань (x = 20) - проходит через точку (20, 70, 40)
        selection.setFacePoint(FaceType.X_MAX, new BlockPos(20, 70, 40));

        // Нижняя грань (y = 64) - проходит через точку (15, 64, 38)
        selection.setFacePoint(FaceType.Y_MIN, new BlockPos(15, 64, 38));

        // Верхняя грань (y = 80) - проходит через точку (12, 80, 42)
        selection.setFacePoint(FaceType.Y_MAX, new BlockPos(12, 80, 42));

        // Передняя грань (z = 30) - проходит через точку (18, 72, 30)
        selection.setFacePoint(FaceType.Z_MIN, new BlockPos(18, 72, 30));

        // Задняя грань (z = 50) - проходит через точку (14, 68, 50)
        selection.setFacePoint(FaceType.Z_MAX, new BlockPos(14, 68, 50));

        // В результате получится параллелепипед:
        // X: [10..20], Y: [64..80], Z: [30..50]

        return selection;
    }

    /**
     * Пример 3: Произвольный порядок установки граней
     * Порядок не имеет значения, можно устанавливать в любой последовательности
     */
    public static Selection createSelectionRandomOrder(Level world) {
        Selection selection = new Selection();
        selection.setWorld(world);

        // Устанавливаем грани в произвольном порядке
        selection.setFacePoint(FaceType.Y_MAX, new BlockPos(100, 200, 100));
        selection.setFacePoint(FaceType.X_MIN, new BlockPos(50, 150, 80));
        selection.setFacePoint(FaceType.Z_MAX, new BlockPos(60, 180, 120));
        selection.setFacePoint(FaceType.Y_MIN, new BlockPos(70, 100, 90));
        selection.setFacePoint(FaceType.X_MAX, new BlockPos(150, 170, 110));
        selection.setFacePoint(FaceType.Z_MIN, new BlockPos(80, 160, 70));

        return selection;
    }

    /**
     * Пример 4: САМЫЙ ПРОСТОЙ СПОСОБ - автоматическое определение bounding box
     * Вы просто даёте 6 точек, система сама находит min/max для каждой оси
     */
    public static Selection createSelectionAuto(Level world) {
        Selection selection = new Selection();

        // Просто даём 6 произвольных точек (или больше, или меньше - не важно)
        // Система автоматически найдёт минимальные и максимальные координаты
        BlockPos[] points = {
                new BlockPos(10, 65, 35), // Точка 1
                new BlockPos(20, 70, 40), // Точка 2
                new BlockPos(15, 64, 38), // Точка 3
                new BlockPos(12, 80, 42), // Точка 4
                new BlockPos(18, 72, 30), // Точка 5
                new BlockPos(14, 68, 50) // Точка 6
        };

        // Вызываем setFromPoints - он автоматически вычислит:
        // minX = 10, maxX = 20
        // minY = 64, maxY = 80
        // minZ = 30, maxZ = 50
        selection.setFromPoints(world, points);

        // Готово! Параллелепипед построен: X[10..20] Y[64..80] Z[30..50]
        return selection;
    }

    /**
     * Пример 5: Использование с переменным количеством точек
     * Можно передать любое количество точек - хоть 2, хоть 100
     */
    public static Selection createFromAnyNumberOfPoints(Level world) {
        Selection selection = new Selection();

        // Метод работает с любым количеством точек
        // Минимум 1 точка, максимум - без ограничений
        selection.setFromPoints(world,
                new BlockPos(5, 60, 10),
                new BlockPos(25, 90, 55),
                new BlockPos(15, 75, 30)
        // ... можно добавить сколько угодно точек
        );

        return selection;
    }

    /**
     * Пример 6: Использование метода setFromCorners (2 точки)
     */
    public static Selection createSelectionFromCorners(Level world) {
        Selection selection = new Selection();
        selection.setWorld(world);

        // Устанавливаем выделение по двум противоположным углам
        selection.setFromCorners(
                new BlockPos(10, 64, 30),
                new BlockPos(20, 80, 50));

        return selection;
    }

    /**
     * Пример 7: Использование метода setFromSixPoints (6 точек)
     */
    public static Selection createSelectionFromSixPoints(Level world) {
        Selection selection = new Selection();
        selection.setWorld(world);

        // Устанавливаем выделение ровно по 6 точкам
        selection.setFromSixPoints(
                new BlockPos(10, 65, 35),
                new BlockPos(20, 70, 40),
                new BlockPos(15, 64, 38),
                new BlockPos(12, 80, 42),
                new BlockPos(18, 72, 30),
                new BlockPos(14, 68, 50));

        return selection;
    }

    /**
     * Вывод информации о выделении
     */
    public static void printSelectionInfo(Selection selection) {
        System.out.println("=== Информация о выделении ===");
        System.out.println(selection.getInfo());
        System.out.println("Минимальная точка: " + selection.getMin());
        System.out.println("Максимальная точка: " + selection.getMax());
        System.out.println("Объём: " + selection.getVolume() + " блоков");
    }
}
