package net.blockhost.anarchyclient.setting;

import java.util.List;

public interface TextValueSetting {

    String valueString();

    void valueFromString(String value);

    default List<String> suggestions() {
        return List.of();
    }
}
