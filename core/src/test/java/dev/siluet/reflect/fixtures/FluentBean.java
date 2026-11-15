package dev.siluet.reflect.fixtures;

/**
 * Fluent accessor backed by a field of the same name (Lombok {@code @Accessors(fluent = true)} style).
 * Worked before #7 and must keep working.
 */
public class FluentBean {
    private final String title = "fluent title";
    private int resetCalls = 0;

    public String title() { return title; }

    /** Fluent-looking method WITHOUT a same-named field: not an accessor, never invoked. */
    public String reset() {
        resetCalls++;
        return "reset";
    }

    public int resetCalls() { return resetCalls; }
}
