package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
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
}
