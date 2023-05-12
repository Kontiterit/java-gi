package io.github.jwharm.javagi.util;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;

import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.glib.Variant;
import org.gnome.gobject.*;

import io.github.jwharm.javagi.base.Bitfield;
import io.github.jwharm.javagi.base.Enumeration;

/**
 * Utility functions to convert a {@link Value} to and from a Java {@link Object}.
 */
public class ValueUtil {
    
    private static final String LOG_DOMAIN = "java-gi";

    /**
     * Read the GType from the GValue, call the corresponding getter (using the methods defined 
     * in the {@link Value} proxy class), and return the result.
     * @param  src a GValue instance.
     * @return     a Java object (or boxed primitive value) that has been marshaled from the
     *             GValue, or {@code null} if {@code src} is null.
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
        } else if (type.equals(Type.G_TYPE_POINTER)) {
            return src.getPointer();
        } else if (type.equals(Type.G_TYPE_PARAM)) {
            return src.getParam();
        } else {
            // Boxed value
            return src.getBoxed();
        }
    }

    /**
     * Call the getter of the GValue that corresponds to the type of the GParamSpec, and return
     * the result.
     * @param src   a GValue instance.
     * @param pspec the GParamSpec that specifies the type of the GValue. Should not be {@code null}
     * @return      a Java object (or boxed primitive value) that has been marshaled from the GValue,
     *              or {@code null} if {@code src} is null.
     */
    public static Object valueToObject(Value src, ParamSpec pspec) {
        if (pspec instanceof ParamSpecBoolean) {
            return src.getBoolean();
        } else if (pspec instanceof ParamSpecBoxed) {
            return src.getBoxed();
        } else if (pspec instanceof ParamSpecChar) {
            return src.getSchar();
        } else if (pspec instanceof ParamSpecUChar) {
            return src.getUchar();
        } else if (pspec instanceof ParamSpecDouble) {
            return src.getDouble();
        } else if (pspec instanceof ParamSpecEnum) {
            return src.getEnum();
        } else if (pspec instanceof ParamSpecFlags) {
            return src.getFlags();
        } else if (pspec instanceof ParamSpecFloat) {
            return src.getFloat();
        } else if (pspec instanceof ParamSpecGType) {
            return src.getGtype();
        } else if (pspec instanceof ParamSpecInt) {
            return src.getInt();
        } else if (pspec instanceof ParamSpecUInt) {
            return src.getUint();
        } else if (pspec instanceof ParamSpecUnichar) {
            return src.getUint();
        } else if (pspec instanceof ParamSpecInt64) {
            return src.getInt64();
        } else if (pspec instanceof ParamSpecUInt64) {
            return src.getUint64();
        } else if (pspec instanceof ParamSpecLong) {
            return src.getLong();
        } else if (pspec instanceof ParamSpecULong) {
            return src.getUlong();
        } else if (pspec instanceof ParamSpecObject) {
            return src.getObject();
        } else if (pspec instanceof ParamSpecParam) {
            return src.getParam();
        } else if (pspec instanceof ParamSpecPointer) {
            return src.getPointer();
        } else if (pspec instanceof ParamSpecString) {
            return src.getString();
        } else if (pspec instanceof ParamSpecVariant) {
            return src.getVariant();
        }
        return null;
    }

    /**
     * Read the GType of the {@code dest} GValue and set the {@code src} object (or boxed primitive 
     * value) as its value using the corresponding setter in the {@link Value} proxy class.
     * @param src  the Java Object (or boxed primitive value) to put in the GValue. Should not be 
     *             {@code null}
     * @param dest the GValue to write to. Should not be {@code null}
     */
    public static void objectToValue(Object src, Value dest) {
        if (src == null || dest == null) {
            return;
        }
        
        Type type = dest.readGType();
        
        if (type == null) {
            return;
        }
        
        try {
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
                // On Linux: Value.setLong(long), on Windows: Value.setLong(int)
                // Use reflection to bypass the type checker
                for (Method m : Value.class.getDeclaredMethods()) {
                    if ("setLong".equals(m.getName())) {
                        m.invoke(dest, src);
                        break;
                    }
                }
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
            } else if (type.equals(Type.G_TYPE_POINTER)) {
                dest.setPointer((MemorySegment) src);
            } else if (type.equals(Type.G_TYPE_PARAM)) {
                dest.setParam((ParamSpec) src);
            } else {
                // Boxed value
                dest.setBoxed((MemorySegment) src);
            }
        } catch (Exception e) {
            GLib.log(
                    LOG_DOMAIN,
                    LogLevelFlags.LEVEL_CRITICAL,
                    "ValueUtil: Cannot set Object with Class %s to GValue with GType %s: %s\n",
                    src.getClass().getSimpleName(),
                    GObjects.typeName(type),
                    e.toString()
            );
        }
    }

    /**
     * Set the {@code src} object (or boxed primitive value) to the GValue using the setter that
     * corresponds to the type of ParamSpec.
     * @param src   the Java Object (or boxed primitive value) to put in the GValue. Should not be
     *              {@code null}
     * @param dest  the GValue to write to. Should not be {@code null}
     * @param pspec the GParamSpec that specifies the type of the GValue. Should not be {@code null}
     */
    public static void objectToValue(Object src, Value dest, ParamSpec pspec) {
        try {
            if (pspec instanceof ParamSpecBoolean) {
                dest.setBoolean((Boolean) src);
            } else if (pspec instanceof ParamSpecBoxed) {
                dest.setBoxed((MemorySegment) src);
            } else if (pspec instanceof ParamSpecChar) {
                dest.setSchar((Byte) src);
            } else if (pspec instanceof ParamSpecUChar) {
                dest.setUchar((Byte) src);
            } else if (pspec instanceof ParamSpecDouble) {
                dest.setDouble((Double) src);
            } else if (pspec instanceof ParamSpecEnum) {
                dest.setEnum(((Enumeration) src).getValue());
            } else if (pspec instanceof ParamSpecFlags) {
                dest.setFlags(((Bitfield) src).getValue());
            } else if (pspec instanceof ParamSpecFloat) {
                dest.setFloat((Float) src);
            } else if (pspec instanceof ParamSpecGType) {
                dest.setGtype((Type) src);
            } else if (pspec instanceof ParamSpecInt) {
                dest.setInt((Integer) src);
            } else if (pspec instanceof ParamSpecUInt) {
                dest.setUint((Integer) src);
            } else if (pspec instanceof ParamSpecUnichar) {
                dest.setUint((Integer) src);
            } else if (pspec instanceof ParamSpecInt64) {
                dest.setInt64((Long) src);
            } else if (pspec instanceof ParamSpecUInt64) {
                dest.setUint64((Long) src);
            } else if (pspec instanceof ParamSpecLong) {
                // On Linux: Value.setLong(long), on Windows: Value.setLong(int)
                // Use reflection to bypass the type checker
                for (Method m : Value.class.getDeclaredMethods()) {
                    if ("setLong".equals(m.getName())) {
                        m.invoke(dest, src);
                        break;
                    }
                }
            } else if (pspec instanceof ParamSpecULong) {
                // On Linux: Value.setLong(long), on Windows: Value.setLong(int)
                // Use reflection to bypass the type checker
                for (Method m : Value.class.getDeclaredMethods()) {
                    if ("setULong".equals(m.getName())) {
                        m.invoke(dest, src);
                        break;
                    }
                }
            } else if (pspec instanceof ParamSpecObject) {
                dest.setObject((GObject) src);
            } else if (pspec instanceof ParamSpecParam) {
                dest.setParam((ParamSpec) src);
            } else if (pspec instanceof ParamSpecPointer) {
                dest.setPointer((MemorySegment) src);
            } else if (pspec instanceof ParamSpecString) {
                dest.setString((String) src);
            } else if (pspec instanceof ParamSpecVariant) {
                dest.setVariant((Variant) src);
            }
        } catch (Exception e) {
            GLib.log(
                    LOG_DOMAIN,
                    LogLevelFlags.LEVEL_CRITICAL,
                    "ValueUtil: Cannot set Object with Class %s to GValue with ParamSpec %s: %s\n",
                    src.getClass().getSimpleName(),
                    pspec.getName(),
                    e.toString()
            );
        }
    }
}