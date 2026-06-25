package net.blockhost.anarchyclient.setting;

public final class StringListSetting extends AbstractStringListSetting {

    private StringListSetting(final StringListSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases(), spec.suggestions());
    }

    public static ImmutableStringListSettingSpec.IdBuildStage builder() {
        return ImmutableStringListSettingSpec.builder();
    }

    public static StringListSetting from(final StringListSettingSpec spec) {
        return new StringListSetting(spec);
    }
}
