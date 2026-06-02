package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.platform.NativeImage;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class NyanCatGifSpammerModule extends Module {

    private static final Identifier GIF_RESOURCE = Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "textures/nyan_cat.gif");
    private static final int TICKS_PER_SECOND = 20;

    private final NumberSetting minIntervalSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_interval_seconds")
            .name("Min Interval")
            .defaultValue(3.0)
            .min(1.0)
            .max(120.0)
            .step(1.0)
            .build()));
    private final NumberSetting maxIntervalSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_interval_seconds")
            .name("Max Interval")
            .defaultValue(12.0)
            .min(1.0)
            .max(120.0)
            .step(1.0)
            .build()));
    private final NumberSetting durationSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("duration_seconds")
            .name("Duration")
            .defaultValue(3.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final NumberSetting scale = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("scale")
            .name("Scale")
            .defaultValue(0.35)
            .min(0.1)
            .max(1.0)
            .step(0.05)
            .build()));
    private final Random random = new Random();
    private int cooldownTicks;
    private int activeTicks;
    private int x = -1;
    private int y = -1;
    private Animation animation;

    public NyanCatGifSpammerModule() {
        super("nyan_cat_gif_spammer", "Nyan Cat GIF Spammer", ModuleCategory.FUN);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null) {
            this.clearAppearance();
            return;
        }
        if (this.activeTicks > 0) {
            this.activeTicks--;
            if (this.activeTicks == 0) {
                this.scheduleNext();
            }
            return;
        }
        if (this.cooldownTicks <= 0) {
            this.startAppearance();
            return;
        }
        this.cooldownTicks--;
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (!this.enabled() || this.activeTicks <= 0 || client.screen instanceof AnarchyClientScreen) {
            return;
        }
        Animation loadedAnimation = this.animation;
        if (loadedAnimation == null) {
            loadedAnimation = loadAnimation(client);
            this.animation = loadedAnimation;
        }
        if (loadedAnimation.frames().isEmpty()) {
            return;
        }

        int width = Math.max(1, (int) Math.round(loadedAnimation.width() * this.scale.value()));
        int height = Math.max(1, (int) Math.round(loadedAnimation.height() * this.scale.value()));
        if (this.x < 0 || this.y < 0) {
            this.x = randomCoordinate(graphics.guiWidth(), width);
            this.y = randomCoordinate(graphics.guiHeight(), height);
        }

        Frame frame = loadedAnimation.currentFrame(System.currentTimeMillis());
        graphics.blit(RenderPipelines.GUI_TEXTURED, frame.texture(), this.x, this.y, 0, 0, width, height, width, height);
    }

    @Override
    protected void onEnable() {
        this.scheduleNext();
    }

    @Override
    protected void onDisable() {
        this.clearAppearance();
    }

    int activeTicks() {
        return this.activeTicks;
    }

    int cooldownTicks() {
        return this.cooldownTicks;
    }

    private void startAppearance() {
        this.activeTicks = secondsToTicks(this.durationSeconds.value());
        this.cooldownTicks = 0;
        this.x = -1;
        this.y = -1;
    }

    private void scheduleNext() {
        this.activeTicks = 0;
        this.cooldownTicks = randomIntervalTicks(this.minIntervalSeconds.value(), this.maxIntervalSeconds.value(), this.random);
        this.x = -1;
        this.y = -1;
    }

    private void clearAppearance() {
        this.activeTicks = 0;
        this.cooldownTicks = 0;
        this.x = -1;
        this.y = -1;
    }

    static int randomIntervalTicks(final double minSeconds, final double maxSeconds, final Random random) {
        int minTicks = secondsToTicks(Math.min(minSeconds, maxSeconds));
        int maxTicks = secondsToTicks(Math.max(minSeconds, maxSeconds));
        if (maxTicks <= minTicks) {
            return minTicks;
        }
        return random.nextInt(minTicks, maxTicks + 1);
    }

    static int secondsToTicks(final double seconds) {
        return Math.max(1, (int) Math.round(seconds * TICKS_PER_SECOND));
    }

    private static int randomCoordinate(final int available, final int size) {
        int max = Math.max(0, available - size);
        return max == 0 ? 0 : ThreadLocalRandom.current().nextInt(max + 1);
    }

    private static Animation loadAnimation(final Minecraft client) {
        try (InputStream stream = client.getResourceManager().open(GIF_RESOURCE);
             ImageInputStream imageStream = ImageIO.createImageInputStream(stream)) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(imageStream, false);
            List<Frame> frames = new ArrayList<>();
            int frameCount = reader.getNumImages(true);
            int totalDurationMillis = 0;
            int width = 1;
            int height = 1;

            for (int index = 0; index < frameCount; index++) {
                BufferedImage image = normalize(reader.read(index));
                width = image.getWidth();
                height = image.getHeight();
                int delayMillis = frameDelayMillis(reader.getImageMetadata(index));
                totalDurationMillis += delayMillis;

                NativeImage nativeImage = toNativeImage(image);
                int frameIndex = index;
                Identifier textureId = Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "nyan_cat/frame_" + frameIndex);
                DynamicTexture texture = new DynamicTexture(() -> "AnarchyClient Nyan Cat " + frameIndex, nativeImage);
                client.getTextureManager().register(textureId, texture);
                frames.add(new Frame(textureId, Math.max(20, delayMillis)));
            }
            reader.dispose();
            return new Animation(frames, width, height, Math.max(20, totalDurationMillis));
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to load Nyan Cat GIF overlay", exception);
            return Animation.EMPTY;
        }
    }

    private static BufferedImage normalize(final BufferedImage source) {
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return image;
    }

    private static NativeImage toNativeImage(final BufferedImage image) {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), true);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                nativeImage.setPixelABGR(x, y, argbToAbgr(image.getRGB(x, y)));
            }
        }
        return nativeImage;
    }

    private static int argbToAbgr(final int argb) {
        return argb & 0xFF00FF00
                | (argb & 0x00FF0000) >> 16
                | (argb & 0x000000FF) << 16;
    }

    private static int frameDelayMillis(final IIOMetadata metadata) {
        Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
        Node extension = findNode(root, "GraphicControlExtension");
        if (extension == null) {
            return 100;
        }
        NamedNodeMap attributes = extension.getAttributes();
        Node delay = attributes.getNamedItem("delayTime");
        if (delay == null) {
            return 100;
        }
        try {
            return Math.max(20, Integer.parseInt(delay.getNodeValue()) * 10);
        } catch (NumberFormatException ignored) {
            return 100;
        }
    }

    private static Node findNode(final Node node, final String name) {
        if (node.getNodeName().equalsIgnoreCase(name)) {
            return node;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Node match = findNode(child, name);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private record Frame(Identifier texture, int durationMillis) {
    }

    private record Animation(List<Frame> frames, int width, int height, int totalDurationMillis) {

        private static final Animation EMPTY = new Animation(List.of(), 1, 1, 1);

        private Frame currentFrame(final long timeMillis) {
            int elapsed = (int) Math.floorMod(timeMillis, this.totalDurationMillis);
            int cursor = 0;
            for (Frame frame : this.frames) {
                cursor += frame.durationMillis();
                if (elapsed < cursor) {
                    return frame;
                }
            }
            return this.frames.getLast();
        }
    }

}
