package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.DESCRIPTION_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteOptions;
import net.lenni0451.rivet.math.Size;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class SettingRowTest {

    private static final Size SIZE = new Size(400, 600);
    private static final String DESCRIPTION = "Explains what this setting does.";

    @Test
    void rowsWithoutDescriptionDropTheDescriptionLine() {
        List<Setting<?>> described = List.of(
                BooleanSetting.from(BooleanSetting.builder()
                        .id("flag")
                        .name("Flag")
                        .defaultValue(true)
                        .description(DESCRIPTION)
                        .build()),
                NumberSetting.from(NumberSetting.builder()
                        .id("range")
                        .name("Range")
                        .defaultValue(4.0)
                        .min(1.0)
                        .max(6.0)
                        .step(0.5)
                        .description(DESCRIPTION)
                        .build()));
        List<Setting<?>> plain = List.of(
                BooleanSetting.from(BooleanSetting.builder()
                        .id("flag")
                        .name("Flag")
                        .defaultValue(true)
                        .build()),
                NumberSetting.from(NumberSetting.builder()
                        .id("range")
                        .name("Range")
                        .defaultValue(4.0)
                        .min(1.0)
                        .max(6.0)
                        .step(0.5)
                        .build()));

        for (int index = 0; index < described.size(); index++) {
            float withDescription =
                    attach(row(described.get(index))).computeIdealSize(SIZE).height();
            float withoutDescription =
                    attach(row(plain.get(index))).computeIdealSize(SIZE).height();
            assertEquals(
                    withDescription - DESCRIPTION_HEIGHT,
                    withoutDescription,
                    0.01F,
                    described.get(index).id());
        }
    }

    @Test
    void rowsLayOutCleanlyWithAndWithoutDescription() {
        List<Setting<?>> settings = List.of(
                BooleanSetting.from(BooleanSetting.builder()
                        .id("described")
                        .name("Described")
                        .defaultValue(true)
                        .description(DESCRIPTION)
                        .build()),
                BooleanSetting.from(BooleanSetting.builder()
                        .id("plain")
                        .name("Plain")
                        .defaultValue(true)
                        .build()),
                NumberSetting.from(NumberSetting.builder()
                        .id("number")
                        .name("Number")
                        .defaultValue(4.0)
                        .min(1.0)
                        .max(6.0)
                        .step(0.5)
                        .build()),
                StringSetting.from(StringSetting.builder()
                        .id("text")
                        .name("Text")
                        .defaultValue("value")
                        .build()));
        Rivet rivet = new Rivet(new TestRivetBackend(), AbsoluteLayout.INSTANCE, SIZE);
        float y = 0F;
        for (Setting<?> setting : settings) {
            Component row = row(setting);
            rivet.root().addChild(row);
            float height = row.computeIdealSize(SIZE).height();
            row.layoutOptions(new AbsoluteOptions(0, y, SIZE.width(), height));
            y += height;
        }
        rivet.root().computeLayout(SIZE);

        String snapshot = LayoutTreeDumper.snapshot(rivet);

        assertTrue(snapshot.contains("issues=0"), snapshot);
        assertTrue(snapshot.contains(DESCRIPTION), snapshot);
    }

    private static Component row(final Setting<?> setting) {
        return PanelWidgets.settingRow(setting, () -> {}, () -> {});
    }

    private static Component attach(final Component row) {
        new Rivet(new TestRivetBackend(), AbsoluteLayout.INSTANCE, SIZE).root().addChild(row);
        return row;
    }
}
