package net.blockhost.anarchyclient.setting;

public final class PacketListSetting extends AbstractStringListSetting {

    private PacketListSetting(final PacketListSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases(), spec.suggestions());
    }

    public static ImmutablePacketListSettingSpec.IdBuildStage builder() {
        return ImmutablePacketListSettingSpec.builder();
    }

    public static PacketListSetting from(final PacketListSettingSpec spec) {
        return new PacketListSetting(spec);
    }

    @Override
    protected String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
