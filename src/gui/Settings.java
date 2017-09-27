package gui;

import java.util.HashMap;

import gui.Settings.Setting;

public class Settings extends HashMap<Setting, Object> {

    public enum SettingSection {
        ALGORITHM("Algorithm options"), DRAW("Draw options"), MISC("Miscellaneous");

        private final String name;

        private SettingSection(String name) {
            this.name= name;
        }


        public String getName() {
            return name;
        }
    }

    public enum Setting {
        DEBUG(SettingSection.ALGORITHM, "Debug", Boolean.FALSE),
        DRAW_CELLS(SettingSection.DRAW, "Draw cells", Boolean.TRUE, true),
        DRAW_CENTERS(SettingSection.DRAW, "Draw glyph centers", Boolean.TRUE, true),
        DRAW_GLYPHS(SettingSection.DRAW, "Draw glyph outlines", Boolean.TRUE, true),
        SHOW_COORDS(SettingSection.MISC, "Show coordinates on mouse over", Boolean.TRUE),
        STEP(SettingSection.ALGORITHM, "Step through", Boolean.FALSE);

        private final Object defaultValue;
        private final String name;
        private final SettingSection section;
        private final boolean triggersRepaint;

        private Setting(SettingSection section, String name, Object defaultValue) {
            this(section, name, defaultValue, false);
        }

        private Setting(SettingSection section, String name, Object defaultValue,
                boolean triggersRepaint) {
            this.defaultValue = defaultValue;
            this.name = name;
            this.section = section;
            this.triggersRepaint = triggersRepaint;
        }


        public Object getDefaultValue() {
            return defaultValue;
        }

        public SettingSection getSection() {
            return section;
        }

        @Override
        public String toString() {
            return name;
        }

        public boolean triggersRepaint() {
            return triggersRepaint;
        }


        public static Setting[] booleanSettings(SettingSection filter) {
            int count = 0;
            for (Setting setting : values()) {
                if ((filter == null || setting.section == filter) &&
                        setting.defaultValue instanceof Boolean) {
                    count++;
                }
            }
            Setting[] result = new Setting[count];
            count = 0;
            for (Setting setting : values()) {
                if ((filter == null || setting.section == filter) &&
                        setting.defaultValue instanceof Boolean) {
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
