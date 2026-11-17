package net.blockhost.anarchyclient.setting;

import java.util.regex.Pattern;

public final class ModuleListSetting extends AbstractStringListSetting {

    private ModuleListSetting(final ModuleListSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases(), spec.suggestions());
    }

    public static ImmutableModuleListSettingSpec.IdBuildStage builder() {
        return ImmutableModuleListSettingSpec.builder();
    }

    public static ModuleListSetting from(final ModuleListSettingSpec spec) {
        return new ModuleListSetting(spec);
    }

    @Override
    protected Pattern separator() {
        return IDENTIFIER_SEPARATOR;
    }
}
