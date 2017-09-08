package gui;

import java.util.HashMap;

import gui.Settings.Setting;

public class Settings extends HashMap<Setting, Object> {

    public enum Setting {
        DEBUG(Boolean.FALSE);

        private final Object defaultValue;

        private Setting(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    public Settings() {
        super();
        for (Setting setting : Setting.values()) {
            put(setting, setting.getDefaultValue());
        }
    }


    public boolean getBoolean(Setting setting) {
        Object o = get(setting);
        if (!(o instanceof Boolean)) {
            throw new IllegalArgumentException();
        }
        return ((Boolean) o).booleanValue();
    }

    public void set(Setting setting, boolean value) {
        put(setting, Boolean.valueOf(value));
    }

    public void toggle(Setting setting) {
        set(setting, !getBoolean(setting));
    }

}
