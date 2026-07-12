package net.createmod.catnip.data;

public enum TriState {
    TRUE, DEFAULT, FALSE;
    public boolean isTrue() { return this == TRUE; }
    public boolean isDefault() { return this == DEFAULT; }
    public boolean isFalse() { return this == FALSE; }
    public boolean getValue() {
        if (this == DEFAULT) throw new IllegalStateException("DEFAULT has no boolean value");
        return this == TRUE;
    }
    public static TriState of(boolean value) { return value ? TRUE : FALSE; }
}
