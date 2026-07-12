package net.createmod.catnip.config;

public enum ConfigType {
    CLIENT, COMMON, SERVER;
    public static ConfigType parse(String value) {
        for (ConfigType type : values()) if (type.name().equalsIgnoreCase(value)) return type;
        throw new IllegalArgumentException("Unknown config type: " + value);
    }
}
