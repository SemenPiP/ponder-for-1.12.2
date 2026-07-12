package net.createmod.catnip.config.ui;

/** Metadata encoded in Forge Configuration comments for compatible config tools. */
public final class ConfigAnnotations {
    private ConfigAnnotations() {
    }

    public enum IntDisplay implements ConfigAnnotation {
        HEX("#"), ZERO_X("0x"), ZERO_B("0b");

        private final String value;
        IntDisplay(String value) { this.value = value; }
        @Override public String getName() { return "IntDisplay"; }
        @Override public String getValue() { return value; }
    }

    public enum RequiresRestart implements ConfigAnnotation {
        CLIENT("client"), SERVER("server"), BOTH("both");

        private final String value;
        RequiresRestart(String value) { this.value = value; }
        @Override public String getName() { return "RequiresRestart"; }
        @Override public String getValue() { return value; }
    }

    public enum RequiresRelog implements ConfigAnnotation {
        TRUE;
        @Override public String getName() { return "RequiresRelog"; }
    }

    public static final class Execute implements ConfigAnnotation {
        private final String command;
        private Execute(String command) {
            if (command == null || command.trim().isEmpty()) throw new IllegalArgumentException("command");
            this.command = command;
        }
        public static Execute run(String command) { return new Execute(command); }
        @Override public String getName() { return "Execute"; }
        @Override public String getValue() { return command; }
    }

    public interface ConfigAnnotation {
        String getName();
        default String getValue() { return null; }
        default String asComment() {
            String value = getValue();
            return "[@cui:" + getName() + (value == null ? "" : ":" + value) + "]";
        }
    }
}
