package org.protelis.vm;

/**
 * The {@link ExecutionEnvironment} is responsible of managing environment variables.
 *
 */
public interface ExecutionEnvironment {

    /**
     * @param id
     *            the variable name
     * @return true if the variable is present
     */
    boolean has(final String id);

    /**
     * @param id
     *            the variable name
     * @return the value of the variable if present, false otherwise
     */
    Object get(final String id);

    /**
     * @param id
     *            the variable name
     * @param defaultValue
     *            a parameterizable default value
     * @return the value of the variable if present, defaultValue otherwise
     */
    Object get(final String id, final Object defaultValue);

    /**
     * @param id
     *            the variable name
     * @param v
     *            the value that should be associated with id
     * @return true if there was previously a value associated with id, and
     *         false if not.
     */
    boolean put(final String id, final Object v);

    /**
     * @param id
     *            the variable name
     * @return Returns the value to which this map previously associated the
     *         key, or null if the map contained no mapping for the key.
     */
    Object remove(final String id);

    /**
     * Called just after the VM is executed, to finalize information of the
     * execution for the environment.
     */
    void commit();

    /**
     * Called just before the VM is executed, to enable and preparations needed
     * in the environment.
     */
    void setup();


}
