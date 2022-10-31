package io.github.jwharm.javagi.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.jwharm.javagi.model.GirElement;
import io.github.jwharm.javagi.model.RegisteredType;
import io.github.jwharm.javagi.model.Repository;
import io.github.jwharm.javagi.model.Type;

public class Conversions {

    public static Map<String, String> nsLookupTable = new HashMap<>();
    public static Map<String, GirElement> cIdentifierLookupTable;
    public static Map<String, RegisteredType> cTypeLookupTable;
	public static Map<String, Repository> repositoriesLookupTable;

    /** Convert "Gdk" to "org.gtk.gdk" */
    public static String namespaceToJavaPackage(String ns) {
        return Objects.requireNonNullElse(nsLookupTable.get(ns.toLowerCase()), ns);
    }

    /** Convert "identifier_name" to "identifierName" */
    public static String toLowerCaseJavaName(String typeName) {
        return replaceKeywords(toCamelCase(typeName, false));
    }

    /** Convert "GLib.type_name" to "TypeName" */
    public static String toSimpleJavaType(String typeName) {
        if (typeName == null) {
            return null;
        }
        int idx = typeName.indexOf('.');
        if (idx > 0) {
            return toCamelCase(typeName.substring(idx + 1), true);
        } else {
            return toCamelCase(typeName, true);
        }
    }

    /** 
     * Convert "Glib.type_name" to "org.gtk.glib.TypeName". 
     * If the typeName does not contain a namespace, the currentPackage parameter
     * is prepended to the result. For example, {@code toQualifiedJavaType("button", "org.gtk.gtk")}
     * returns {@code "org.gtk.gtk.Button"}.
     */
    public static String toQualifiedJavaType(String typeName, String currentPackage) {
        if (typeName == null) {
            return null;
        }
        if (typeName.equals("VaList")) {
        	return typeName;
        }
        int idx = typeName.indexOf('.');
        if (idx > 0) {
            String namespace = Conversions.namespaceToJavaPackage(typeName.substring(0, idx));
            if (namespace == null) System.err.println("Could not get namespace for " + typeName);
            return namespace + "." + toCamelCase(typeName.substring(idx + 1), true);
        } else {
            return currentPackage + "." + toCamelCase(typeName, true);
        }
    }

    /** Convert "GLib.TypeName" to "org.gtk.glib" */
    public static String getJavaPackageName(String typeName) {
        if (typeName == null) {
            return null;
        }
        int idx = typeName.indexOf('.');
        if (idx > 0) {
            return "org.gtk." + typeName.substring(0, idx).toLowerCase();
        }
        return null;
    }

    public static String toCamelCase(String typeName, boolean startUpperCase) {
        if (typeName == null) {
            return null;
        }
        char[] chars = typeName.toCharArray();
        StringBuilder builder = new StringBuilder();

        boolean upper = startUpperCase;
        for (char c : chars) {
            if (c == '_' || c == '-') {
                upper = true;
            } else {
                builder.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return builder.toString();
    }

    public static String prefixDigits(String name) {
        return Character.isDigit(name.charAt(0)) ? "_" + name : name;
    }

    private static String replaceKeywords(String name) {
        final String[] keywords = new String[] {
                "abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package",
                "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements",
                "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
                "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char",
                "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile",
                "const", "float", "native", "super", "while"
        };
        return (Arrays.stream(keywords).anyMatch(kw -> kw.equalsIgnoreCase(name))) ? name + "_" : name;
    }

    public static String replaceJavaObjectMethodNames(String name) {
        for (java.lang.reflect.Method m : Object.class.getMethods()) {
            if (m.getName().equals(name)) {
                return name + "_";
            }
        }
        return name;
    }

    /**
     * Convert C type declaration into Java type declaration.
     * This does not work correctly in all cases. For example, Java does not support 
     * unsigned data types (like "unsigned short").
     */
    public static String convertToJavaType(String name, boolean qualified, String currentPackage) {
        return name == null ? null : switch (name.toLowerCase()) {
            case "gboolean" -> "boolean";
            case "gchar", "guchar", "gint8", "guint8" -> "byte";
            case "gshort", "gushort", "gint16", "guint16" -> "short";
            case "gint", "guint", "gint32", "guint32", "gunichar" -> "int";
            case "glong", "gulong", "gint64", "gssize", "gsize", "goffset", "guint64" -> "long";
            case "gdouble" -> "double";
            case "gfloat" -> "float";
            case "none" -> "void";
            case "utf8", "filename" -> "java.lang.String";
            case "gpointer", "gconstpointer" -> "java.lang.foreign.MemoryAddress";
            case "gtype" -> qualified ? toQualifiedJavaType("GLib.Type", currentPackage) : toSimpleJavaType("GLib.Type");
            case "VaList", "va_list" -> "VaList";
            case "long double" -> "double"; // unsupported data type
            default -> qualified ? toQualifiedJavaType(name, currentPackage) : toSimpleJavaType(name);
        };
    }

    public static String toPanamaJavaType(Type t) {
        if (t == null) {
            return "MemoryAddress";
        } else if (t.cType != null && t.cType.endsWith("*")) {
            return "MemoryAddress";
        } else if (t.isEnum() || t.isBitfield() || t.isBoolean()) {
            return "int";
        } else if (t.isPrimitive || "void".equals(t.simpleJavaType)) {
            return t.simpleJavaType;
        } else if (t.isAliasForPrimitive()) {
            return t.girElementInstance.type.simpleJavaType;
        } else {
            return "MemoryAddress";
        }
    }

    public static String toPanamaMemoryLayout(Type t) {
        if (t == null) {
            return "ValueLayout.ADDRESS";
        } else if (t.isEnum() || t.isBitfield() || t.isBoolean()) {
            return "ValueLayout.JAVA_INT";
        } else if (t.isPointer()) {
            return "ValueLayout.ADDRESS";
        } else if (t.isPrimitive) {
            return "ValueLayout.JAVA_" + t.simpleJavaType.toUpperCase();
        } else if (t.isAliasForPrimitive()) {
            return "ValueLayout.JAVA_" + t.girElementInstance.type.simpleJavaType.toUpperCase();
        } else {
            return "ValueLayout.ADDRESS";
        }
    }

    public static String getValueLayout(Type t) {
        if (t == null) {
            return "ValueLayout.ADDRESS";
        } else if (t.isEnum() || t.isBitfield() || t.isBoolean()) {
            return "ValueLayout.JAVA_INT";
        } else if (t.isPrimitive) {
            return "ValueLayout.JAVA_" + t.simpleJavaType.toUpperCase();
        } else if (t.isAliasForPrimitive()) {
            return "ValueLayout.JAVA_" + t.girElementInstance.type.simpleJavaType.toUpperCase();
        } else {
            return "ValueLayout.ADDRESS";
        }
    }

    public static boolean isPrimitive(String javaType) {
        return javaType != null
                && List.of("boolean", "byte", "char", "double", "float", "int", "long", "short").contains(javaType);
    }
    
    public static String primitiveClassName(String primitive) {
        return switch(primitive) {
            case "char" -> "Character";
            case "int" -> "Integer";
            default -> toCamelCase(primitive, true);
        };
    }

    public static String literal(String type, String value) throws NumberFormatException {
        return switch (type) {
            case "boolean" -> Boolean.valueOf(value).toString();
            case "byte" -> Byte.valueOf(value).toString();
            case "char" -> "'" + value + "'";
            case "double" -> Double.valueOf(value) + "d";
            case "float" -> Float.valueOf(value) + "f";
            case "int" -> Integer.valueOf(value).toString();
            case "long" -> Long.valueOf(value) + "L";
            case "short" -> Short.valueOf(value).toString();
            case "java.lang.String" -> '"' + value + '"';
            default -> value;
        };
    }
}
