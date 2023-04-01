package io.github.jwharm.javagi.pointer;

import java.lang.foreign.*;
import java.lang.reflect.Array;
import java.util.List;
import java.util.function.Function;

import io.github.jwharm.javagi.base.Proxy;

/**
 * This type of Pointer object points to a GObject-derived object 
 * in native memory.
 * @param <T> The type of the object
 */
public class PointerProxy<T extends Proxy> extends Pointer<T> {

    private final Function<MemorySegment, T> constructor;

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerProxy(MemorySegment address, Function<MemorySegment, T> constructor) {
        super(address);
        this.constructor = constructor;
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(T value) {
        segment.set(ValueLayout.ADDRESS, 0, value.handle());
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return the value of the pointer
     */
    public T get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <p>
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public T get(int index) {
        // Get the memory address of the native object.
        MemorySegment ref = segment.getAtIndex(ValueLayout.ADDRESS, index);
        // Call the constructor of the proxy object and return the created instance.
        return makeInstance(ref);
    }

    /**
     * Read an array of flat struct values from the pointer
     * @param length length of the array
     * @param clazz type of the array elements
     * @return the array
     */
    public T[] toArrayOfStructs(int length, Class<T> clazz, MemoryLayout layout) {
        // clazz is of type Class<T>, so the cast to T is safe
        @SuppressWarnings("unchecked") T[] array = (T[]) Array.newInstance(clazz, length);
        var seg = MemorySegment.ofAddress(segment.address(), length * layout.byteSize(), SegmentScope.auto());
        // MemorySegment.elements() only works for >1 elements
        if (length == 1) {
            array[0] = makeInstance(seg);
        } else {
            List<MemorySegment> elements = seg.elements(layout).toList();
            for (int i = 0; i < length; i++) {
                array[i] = makeInstance(elements.get(i));
            }
        }
        return array;
    }
    
    // Get the constructor and create a new instance.
    // The unchecked warning is suppressed because the constructors are explicitly registered for the correct type.
    private T makeInstance(MemorySegment ref) {
        return constructor.apply(ref);
    }
}
