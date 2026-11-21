package net.blockhost.anarchyclient.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandSupportTest {

    @Test
    void parsesCommandCoordinatesAndRelativeBlockPositions() {
        assertEquals(12.0, CommandSupport.parseCoordinate("12", 5.0));
        assertEquals(5.0, CommandSupport.parseCoordinate("~", 5.0));
        assertEquals(7.5, CommandSupport.parseCoordinate("~2.5", 5.0));
        assertEquals(
                new BlockPos(12, 64, -5),
                CommandSupport.parseBlockPosition("~2", "64", "~-1", new Vec3(10.2, 70.0, -3.4)));
        assertThrows(IllegalArgumentException.class, () -> CommandSupport.parseCoordinate("~bad", 0.0));
    }

    @Test
    void parsesExactPositionsWithoutRounding() {
        assertEquals(
                new Vec3(10.2, 72.5, -3.4), CommandSupport.parsePosition("~", "~2.5", "~", new Vec3(10.2, 70.0, -3.4)));
    }

    @Test
    void coordinateArgumentAcceptsRelativeTokens() throws CommandSyntaxException {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        String[] parsed = new String[3];
        dispatcher.register(LiteralArgumentBuilder.literal("tp")
                .then(RequiredArgumentBuilder.argument("x", CoordinateArgument.coordinate())
                        .then(RequiredArgumentBuilder.argument("y", CoordinateArgument.coordinate())
                                .then(RequiredArgumentBuilder.argument("z", CoordinateArgument.coordinate())
                                        .executes(context -> {
                                            parsed[0] = CoordinateArgument.getCoordinate(context, "x");
                                            parsed[1] = CoordinateArgument.getCoordinate(context, "y");
                                            parsed[2] = CoordinateArgument.getCoordinate(context, "z");
                                            return 1;
                                        })))));

        dispatcher.execute("tp ~ ~-2 12.5", new Object());

        assertArrayEquals(new String[] {"~", "~-2", "12.5"}, parsed);
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("tp ~ up 0", new Object()));
    }

    @Test
    void confinesUserPathsToTheBaseDirectory(@TempDir final Path base) {
        assertEquals(
                base.toAbsolutePath().normalize().resolve("exports/profile.json"),
                CommandSupport.resolveInside(base, "exports/profile.json"));
        assertEquals(
                base.toAbsolutePath().normalize().resolve("profile.json"),
                CommandSupport.resolveInside(base, "exports/../profile.json"));
        assertThrows(IllegalArgumentException.class, () -> CommandSupport.resolveInside(base, "../outside.json"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CommandSupport.resolveInside(
                        base, base.getRoot().resolve("etc").resolve("passwd").toString()));
        assertThrows(IllegalArgumentException.class, () -> CommandSupport.resolveInside(base, "."));
    }

    @Test
    void fillAreasAreNormalizedAndBounded() {
        CommandSupport.BlockArea area = CommandSupport.blockArea(new BlockPos(3, 5, 7), new BlockPos(1, 4, 6), 12);

        assertEquals(1, area.minX());
        assertEquals(4, area.minY());
        assertEquals(6, area.minZ());
        assertEquals(3, area.maxX());
        assertEquals(5, area.maxY());
        assertEquals(7, area.maxZ());
        assertEquals(12, area.count());
        assertThrows(
                IllegalArgumentException.class,
                () -> CommandSupport.blockArea(new BlockPos(0, 0, 0), new BlockPos(3, 3, 3), 8));
    }

    @Test
    void sanitizesSkinFileNames() {
        assertEquals("Bad_Name__", CommandSupport.sanitizeFileName("Bad/Name:*"));
        assertEquals("skin", CommandSupport.sanitizeFileName("   "));
    }

    @Test
    void rejectsCoordinatesThatAreNotFinite() {
        // A NaN position passes every distance check and the server disconnects the player for it.
        assertThrows(IllegalArgumentException.class, () -> CommandSupport.parseCoordinate("NaN", 0.0));
        assertThrows(IllegalArgumentException.class, () -> CommandSupport.parseCoordinate("~NaN", 0.0));
        assertThrows(IllegalArgumentException.class, () -> CommandSupport.parseCoordinate("Infinity", 0.0));
    }
}
