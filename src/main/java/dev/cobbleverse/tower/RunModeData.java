package dev.cobbleverse.tower;

/** Per-run override; null preserves pre-addon/legacy command behavior. */
public interface RunModeData {
    boolean competitiveTower$isOpen();
    void competitiveTower$setOpen(boolean open);
    Boolean competitiveTower$getMode();
    void competitiveTower$setMode(Boolean competitive);
}
