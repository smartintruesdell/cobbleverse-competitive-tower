package dev.cobbleverse.tower;

/** Per-run override; null preserves pre-addon/legacy command behavior. */
public interface RunModeData {
    Boolean competitiveTower$getMode();
    void competitiveTower$setMode(Boolean competitive);
}
