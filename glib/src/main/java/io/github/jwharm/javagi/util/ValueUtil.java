package io.github.jwharm.javagi.util;

import java.lang.foreign.MemoryAddress;

import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;
import org.gnome.gobject.ParamSpec;
import org.gnome.gobject.Value;

import io.github.jwharm.javagi.base.Bitfield;
import io.github.jwharm.javagi.base.Enumeration;

/**
 * Utility functions to convert a {@link Value} to and from a Java {@link Object}.
 */
public class ValueUtil {
    
    /**
     * Read the GType from the GValue, call the corresponding getter (using the methods defined 
     * in the {@link Value} proxy class), and return the result.
     * @param  src a GValue instance.
     * @return a Java object (or boxed primitive value) that has been marshaled from the GValue, 
     *         or {@code null} if {@code src} is null.
     */
    public static Object valueToObject(Value src) {
        if (src == null) {
            return null;
        }
        
        Type type = src.readGType();
        
        if (type == null) {
            return null;
        }
        
        if (type.equals(Type.G_TYPE_BOOLEAN)) {
            return src.getBoolean();
        } else if (type.equals(Type.G_TYPE_CHAR)) {
            return src.getSchar();
        } else if (type.equals(Type.G_TYPE_DOUBLE)) {
            return src.getDouble();
        } else if (type.equals(Type.G_TYPE_FLOAT)) {
            return src.getFloat();
        } else if (type.equals(Type.G_TYPE_INT)) {
            return src.getInt();
        } else if (type.equals(Type.G_TYPE_LONG)) {
            return src.getLong();
        } else if (type.equals(Type.G_TYPE_STRING)) {
            return src.getString();
        } else if (type.equals(Type.G_TYPE_ENUM)) {
            return src.getEnum();
        } else if (type.equals(Type.G_TYPE_FLAGS)) {
            return src.getFlags();
        } else if (type.equals(Type.G_TYPE_OBJECT)) {
            return src.getObject();
        } else if (type.equals(GObjects.gtypeGetType())) {
            return src.getGtype();
        } else if (type.equals(Type.G_TYPE_BOXED)) {
            return src.getBoxed();
        } else if (type.equals(Type.G_TYPE_POINTER)) {
            return src.getPointer();
        } else if (type.equals(Type.G_TYPE_PARAM)) {
            return src.getParam();
        } else {
            return null;
        }
    }
    
    /**
     * Read the GType of the {@code dest} GValue and set the {@code src} object (or boxed primitive 
     * value) as its value using the corresponding setter in the {@link Value} proxy class.
     * @param src  the Java Object (or boxed primitive value) to put in the GValue. Should not be 
     *             {@code null}
     * @param dest the GValue to write to. Should not be {@code null}.
     */
    public static void objectToValue(Object src, Value dest) {
        if (src == null || dest == null) {
            return;
        }
        
        Type type = dest.readGType();
        
        if (type == null) {
            return;
        }
        
        if (type.equals(Type.G_TYPE_BOOLEAN)) {
            dest.setBoolean((Boolean) src);
        } else if (type.equals(Type.G_TYPE_CHAR)) {
            dest.setSchar((Byte) src);
        } else if (type.equals(Type.G_TYPE_DOUBLE)) {
            dest.setDouble((Double) src);
        } else if (type.equals(Type.G_TYPE_FLOAT)) {
            dest.setFloat((Float) src);
        } else if (type.equals(Type.G_TYPE_INT)) {
            dest.setInt((Integer) src);
        } else if (type.equals(Type.G_TYPE_LONG)) {
            dest.setLong((Long) src);
        } else if (type.equals(Type.G_TYPE_STRING)) {
            dest.setString((String) src);
        } else if (type.equals(Type.G_TYPE_ENUM)) {
            dest.setEnum(((Enumeration) src).getValue());
        } else if (type.equals(Type.G_TYPE_FLAGS)) {
            dest.setFlags(((Bitfield) src).getValue());
        } else if (type.equals(Type.G_TYPE_OBJECT)) {
            dest.setObject((GObject) src);
        } else if (type.equals(GObjects.gtypeGetType())) {
            dest.setGtype((Type) src);
        } else if (type.equals(Type.G_TYPE_BOXED)) {
            dest.setBoxed((MemoryAddress) src);
        } else if (type.equals(Type.G_TYPE_POINTER)) {
            dest.setPointer((MemoryAddress) src);
        } else if (type.equals(Type.G_TYPE_PARAM)) {
            dest.setParam((ParamSpec) src);
        }
    }
}
