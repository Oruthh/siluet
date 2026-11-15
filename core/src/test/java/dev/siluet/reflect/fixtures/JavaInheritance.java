package dev.siluet.reflect.fixtures;

/**
 * Visibility corner cases for {@code Method.invoke}.
 */
public final class JavaInheritance {
    private JavaInheritance() {}

    public interface Named {
        String getName();
    }

    /** Public method in a private class: only invokable through {@link Named}. */
    private static final class HiddenNamed implements Named {
        @Override
        public String getName() { return "hidden implementation"; }
    }

    public static Named hiddenNamed() { return new HiddenNamed(); }

    /** Package-private base class with a public getter... */
    static class PackagePrivateBase {
        public String getInherited() { return "inherited value"; }
    }

    /** ...inherited by a public class (javac generates a public bridge here). */
    public static class PublicChild extends PackagePrivateBase {}
}
