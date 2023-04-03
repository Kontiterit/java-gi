package io.github.jwharm.javagi.base;

import org.gnome.glib.GLib;

import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;
import java.lang.reflect.Method;

/**
 * A ProxyInstance subclass on which a {@link Cleaner} can be attached to
 * automatically free native memory when the object is not used anymore.
 */
public class ProxyInstanceCleanable extends ProxyInstance {

    private final StructFinalizer finalizer;

    /**
     * Create a new {@code ProxyInstanceCleanable} object for an instance in native memory.
     * @param address the memory address of the instance
     */
    public ProxyInstanceCleanable(MemorySegment address) {
        super(address);

        // Null check the address
        if (address == null || address.equals(MemorySegment.NULL)) {
            this.finalizer = null;
            return;
        }

        // Use free() method (if any)
        Method freeFunc = null;
        if (this instanceof FreeFunc) {
            try {
                freeFunc = getClass().getDeclaredMethod("free");
            } catch (NoSuchMethodException ignored) {}
        }

        this.finalizer = new StructFinalizer(address, freeFunc);
    }

    /**
     * Enable the {@link Cleaner} to free the native memory
     */
    public void takeOwnership() {
        finalizer.setEnabled(true);
    }

    /**
     * Disable the {@link Cleaner} to free the native memory
     */
    public void yieldOwnership() {
        finalizer.setEnabled(false);
    }

    /**
     * Return the callback that the {@link Cleaner} will run to free native memory
     */
    public Runnable getFinalizer() {
        return finalizer;
    }

    /**
     * This callback is run by the {@link Cleaner} when a struct or union instance
     * has become unreachable, to free the native memory using {@link GLib#free(MemorySegment)}.
     */
    private static class StructFinalizer implements Runnable {

        private final MemorySegment address;
        private final Method freeFunc;
        private boolean enabled;

        public StructFinalizer(MemorySegment address, Method freeFunc) {
            this.address = address;
            this.freeFunc = freeFunc;
            this.enabled = false;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void run() {
            if (enabled) {
                if (freeFunc == null) {
                    GLib.free(address);
                } else {
                    try {
                        freeFunc.invoke(address);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}