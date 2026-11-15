package dev.siluet.reflect.fixtures;

/**
 * Badly named Java bean: the field is called {@code isEnabled} and so is its getter.
 * Before #7 it was reachable as {@code isEnabled} (field rule); that name must keep working,
 * and the same method must NOT additionally become reachable as {@code enabled}.
 */
public class LegacyBean {
    private final boolean isEnabled = true;

    public boolean isEnabled() { return isEnabled; }
}
