package dev.siluet.reflect.fixtures;

/**
 * Record components are exposed through their accessors; other methods are not.
 */
public record JavaRecord(String title, int quantity) {
    public String summary() { return title + " x" + quantity; }
}
