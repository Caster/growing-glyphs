package gui;

import java.util.HashMap;

import gui.Settings.Setting;

public class Settings extends HashMap<Setting, Object> {

    public enum Setting {
        DEBUG("Debug", Boolean.FALSE),
        STEP("Step through", Boolean.FALSE);

        private final Object defaultValue;
        private final String name;

        private Setting(String name, Object defaultValue) {
            this.defaultValue = defaultValue;
            this.name = name;
        }


        public Object getDefaultValue() {
            return defaultValue;
        }

        @Override
        public String toString() {
            return name;
        }


        public static Setting[] booleanSettings() {
            int count = 0;
            for (Setting setting : values()) {
                if (setting.defaultValue instanceof Boolean) {
                    count++;
                }
            }
            Setting[] result = new Setting[count];
            count = 0;
            for (Setting setting : values()) {
                if (setting.defaultValue instanceof Boolean) {
                    result[count++] = setting;
                }
            }
            return result;
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
