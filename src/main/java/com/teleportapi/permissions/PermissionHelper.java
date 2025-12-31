package com.teleportapi.permissions;

import com.teleportapi.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to manage and execute permission checks over areas.
 */
public class PermissionHelper {
    private static final List<IPermissionChecker> CHECKERS = new ArrayList<>();
    private static boolean enabled = true;
    private static boolean checkSource = true;
    private static boolean checkTarget = true;

    static {
        // Register default Forge checker
        registerChecker(new DefaultForgeChecker());
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setCheckSource(boolean value) {
        checkSource = value;
    }

    public static boolean isCheckSource() {
        return checkSource;
    }

    public static void setCheckTarget(boolean value) {
        checkTarget = value;
    }

    public static boolean isCheckTarget() {
        return checkTarget;
    }

    public static void registerChecker(IPermissionChecker checker) {
        CHECKERS.add(checker);
        CHECKERS.sort(Comparator.comparingInt(IPermissionChecker::getPriority).reversed());
    }

    /**
     * Performs an optimized search of the area to check if the player has
     * permissions.
     * Instead of checking every block, it checks strategic points (corners, edges,
     * centers, and per-chunk samples).
     */
    public static CheckResult checkAreaPermissions(@Nullable Player player, Level level, Selection selection,
            boolean checkBreak) {
        if (!enabled)
            return CheckResult.ALLOW;
        if (checkBreak && !checkSource)
            return CheckResult.ALLOW;
        if (!checkBreak && !checkTarget)
            return CheckResult.ALLOW;
        if (!selection.isComplete())
            return CheckResult.ALLOW;

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();

        Set<BlockPos> pointsToCheck = new HashSet<>();

        // 1. Corners (8 points)
        pointsToCheck.add(new BlockPos(min.getX(), min.getY(), min.getZ()));
        pointsToCheck.add(new BlockPos(max.getX(), min.getY(), min.getZ()));
        pointsToCheck.add(new BlockPos(min.getX(), max.getY(), min.getZ()));
        pointsToCheck.add(new BlockPos(min.getX(), min.getY(), max.getZ()));
        pointsToCheck.add(new BlockPos(max.getX(), max.getY(), min.getZ()));
        pointsToCheck.add(new BlockPos(max.getX(), min.getY(), max.getZ()));
        pointsToCheck.add(new BlockPos(min.getX(), max.getY(), max.getZ()));
        pointsToCheck.add(new BlockPos(max.getX(), max.getY(), max.getZ()));

        // 2. Centers (1 point)
        pointsToCheck.add(new BlockPos(
                min.getX() + (max.getX() - min.getX()) / 2,
                min.getY() + (max.getY() - min.getY()) / 2,
                min.getZ() + (max.getZ() - min.getZ()) / 2));

        // 3. Per-chunk samples
        // This ensures that if the area spans multiple claims (which usually follow
        // chunk boundaries),
        // we check at least one point in each chunk.
        for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
                int sampleX = Math.max(min.getX(), Math.min(max.getX(), (cx << 4) + 8));
                int sampleZ = Math.max(min.getZ(), Math.min(max.getZ(), (cz << 4) + 8));
                // Sample at multiple heights if the area is vertically large
                for (int cy = min.getY() >> 4; cy <= max.getY() >> 4; cy++) {
                    int sampleY = Math.max(min.getY(), Math.min(max.getY(), (cy << 4) + 8));
                    pointsToCheck.add(new BlockPos(sampleX, sampleY, sampleZ));
                }
            }
        }

        // Execute checks
        for (BlockPos pos : pointsToCheck) {
            for (IPermissionChecker checker : CHECKERS) {
                boolean allowed = checkBreak ? checker.canBreak(player, level, pos)
                        : checker.canPlace(player, level, pos);
                if (!allowed) {
                    return new CheckResult(false, pos, "Denied by " + checker.getClass().getSimpleName());
                }
            }
        }

        return CheckResult.ALLOW;
    }

    public static class CheckResult {
        public static final CheckResult ALLOW = new CheckResult(true, null, null);

        private final boolean allowed;
        private final BlockPos failedPos;
        private final String reason;

        public CheckResult(boolean allowed, @Nullable BlockPos failedPos, @Nullable String reason) {
            this.allowed = allowed;
            this.failedPos = failedPos;
            this.reason = reason;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public BlockPos getFailedPos() {
            return failedPos;
        }

        public String getReason() {
            return reason;
        }
    }
}
