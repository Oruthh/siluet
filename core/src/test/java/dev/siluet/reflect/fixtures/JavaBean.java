package dev.siluet.reflect.fixtures;

/**
 * Classic Java bean used by the property accessor tests (issue #7).
 */
public class JavaBean {
    private final boolean active = true;
    private int cancelCalls = 0;

    /** JavaBeans boolean getter: exposed as {@code active}. */
    public boolean isActive() { return active; }

    /** Boxed Boolean getter without a field: exposed as {@code verified}. */
    public Boolean isVerified() { return Boolean.TRUE; }

    /** Getter without a backing field: exposed as {@code computed}. */
    public String getComputed() { return "computed value"; }

    /** Two leading capitals: exposed as {@code URL}, not {@code uRL}. */
    public String getURL() { return "https://siluet.dev"; }

    /** {@code is} prefix on a non-boolean in Java is not a getter: never exposed. */
    public String isNotBoolean() { return "not a getter"; }

    /** Sensitive value used by blockedMembers tests. */
    public String getPasswordHash() { return "secret-hash"; }

    /** Not a getter, has a side effect: must never be invoked from a template. */
    public boolean cancel() {
        cancelCalls++;
        return true;
    }

    /** Static getter: must never be exposed. */
    public static int getInstanceCount() { return 42; }

    public int cancelCalls() { return cancelCalls; }
}
