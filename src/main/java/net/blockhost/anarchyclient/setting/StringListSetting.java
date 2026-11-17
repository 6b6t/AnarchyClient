package net.blockhost.anarchyclient.setting;

import java.util.regex.Pattern;

public final class StringListSetting extends AbstractStringListSetting {

    private final Pattern separator;

    private StringListSetting(final StringListSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases(), spec.suggestions());
        this.separator = spec.splitOnWhitespace() ? IDENTIFIER_SEPARATOR : TEXT_SEPARATOR;
    }

    public static ImmutableStringListSettingSpec.IdBuildStage builder() {
        return ImmutableStringListSettingSpec.builder();
    }

    public static StringListSetting from(final StringListSettingSpec spec) {
        return new StringListSetting(spec);
    }

    @Override
    protected Pattern separator() {
        return this.separator;
    }
}
