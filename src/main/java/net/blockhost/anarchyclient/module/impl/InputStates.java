package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.ClientInputAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;

final class InputStates {

    private InputStates() {
    }

    static boolean keyPressed(final Minecraft client, final int key) {
        return client.getWindow() != null && GLFW.glfwGetKey(client.getWindow().handle(), key) == GLFW.GLFW_PRESS;
    }

    static boolean mousePressed(final Minecraft client, final int button) {
        return client.getWindow() != null && GLFW.glfwGetMouseButton(client.getWindow().handle(), button) == GLFW.GLFW_PRESS;
    }

    static Input withShift(final Input input, final boolean shift) {
        return new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), shift, input.sprint());
    }

    static Input withJump(final Input input, final boolean jump) {
        return new Input(input.forward(), input.backward(), input.left(), input.right(), jump, input.shift(), input.sprint());
    }

    static Input withForward(final Input input, final boolean forward) {
        return new Input(forward, input.backward(), input.left(), input.right(), input.jump(), input.shift(), input.sprint());
    }

    static Input withBackward(final Input input, final boolean backward) {
        return new Input(input.forward(), backward, input.left(), input.right(), input.jump(), input.shift(), input.sprint());
    }

    static Input withSprint(final Input input, final boolean sprint) {
        return new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.shift(), sprint);
    }

    static boolean moving(final Input input) {
        return input.forward() || input.backward() || input.left() || input.right();
    }

    static void refreshMoveVector(final ClientInput input) {
        ((ClientInputAccessor) input).anarchyclient$setMoveVector(moveVector(input.keyPresses));
    }

    static Vec2 moveVector(final Input input) {
        float forward = impulse(input.forward(), input.backward());
        float right = impulse(input.left(), input.right());
        return new Vec2(right, forward).normalized();
    }

    private static float impulse(final boolean positive, final boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }
}
