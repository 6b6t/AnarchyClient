package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.platform.InputConstants;
import javax.annotation.Nullable;
import net.lenni0451.rivet.backend.Backend;
import net.lenni0451.rivet.backend.text.Font;
import net.lenni0451.rivet.input.keyboard.Key;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtil;

public final class Blaze3DBackend implements Backend {

    private final Minecraft client;
    private final Font font;

    public Blaze3DBackend(final Minecraft client) {
        this.client = client;
        this.font = new MinecraftFont(client.font);
    }

    @Override
    public Font font() {
        return this.font;
    }

    @Override
    @Nullable
    public String getClipboard() {
        // Filter like vanilla's text fields: newlines, tabs and section signs would otherwise end up in settings.
        String clipboard = this.client.keyboardHandler.getClipboard();
        String filtered = clipboard == null ? "" : StringUtil.filterText(clipboard);
        return filtered.isEmpty() ? null : filtered;
    }

    @Override
    public void setClipboard(final String clipboard) {
        this.client.keyboardHandler.setClipboard(clipboard);
    }

    @Override
    public boolean isKeyDown(final Key key) {
        return RivetInputMapper.toScancode(key).stream().anyMatch(InputConstants::isKeyDown);
    }
}
