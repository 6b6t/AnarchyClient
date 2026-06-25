package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.server.ServerObserver;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class FlagTimelineHudModule extends HudElementModule {

    public FlagTimelineHudModule() {
        super("flag_timeline_hud", "Flag Timeline HUD", "Top Right");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        ServerObserver.Snapshot snapshot = ServerObserver.snapshot();
        ServerObserver.FlagInfo flag = snapshot.lastFlag();
        String detail = flag.detail() == null || flag.detail().isBlank() ? "" : " " + flag.detail();
        return List.of(
                "Flags " + snapshot.flagSequence(),
                "Last " + flag.reason() + detail,
                "Velocity " + snapshot.velocityCorrections(),
                "Tab " + snapshot.tabAdds() + "/" + snapshot.tabRemoves()
        );
    }
}
