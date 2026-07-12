package net.createmod.catnip.platform;

public enum Loader {
    FORGE;
    public boolean isForge(){return true;} public boolean isFabric(){return false;} public boolean isNeoForge(){return false;}
    public boolean isCurrent(){return this==CatnipServices.PLATFORM.getLoader();}
}
