package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import java.io.IOException;
import java.io.Writer;

public class Alias extends RegisteredType {

    public Alias(GirElement parent, String name, String cType) {
        super(parent, name, null, cType);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        if (type.isCallback()) {
            writer.write("public interface " + javaName);
        } else {
            writer.write("public class " + javaName);
        }

        // Handle alias for type "none"
        if (type.qualifiedJavaType.equals("void")) {
            writer.write(" extends org.gtk.gobject.Object");
        } else if (inherits()) {
            writer.write(" extends " + type.qualifiedJavaType);
        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write(" extends Alias<" + type.simpleJavaType + ">");
        } else if (type.isPrimitive) {
            writer.write(" extends Alias<" + Conversions.primitiveClassName(type.simpleJavaType) + ">");
        }
        writer.write(" {\n");
        writer.write("\n");

        // Generate standard constructors from a MemoryAddress and a gobject.Object
        if (inherits()) {
            // A record (C Struct) is not a GObject
            if ((type.girElementInstance != null)
                    && (type.girElementInstance.type != null)
                    && (! type.girElementInstance.type.isRecord())) {
                generateCastFromGObject(writer);
            }
            if (! type.isCallback()) {
                generateMemoryAddressConstructor(writer);
            }
        } else {
//            writer.write("    private final " + type.simpleJavaType + " value;\n");
//            writer.write("    \n");
            writer.write("    public " + javaName + "(" + type.simpleJavaType + " value) {\n");
            writer.write("        this.value = value;\n");
            writer.write("    }\n");
            writer.write("    \n");
//            writer.write("    public " + type.simpleJavaType + " getValue() {\n");
//            writer.write("        return this.value;\n");
//            writer.write("    }\n");
//            writer.write("    \n");
            writer.write("    public static " + type.simpleJavaType + "[] getValues(" + javaName + "[] array) {\n");
            writer.write("        " + type.simpleJavaType + "[] values = new " + type.simpleJavaType + "[array.length];\n");
            writer.write("        for (int i = 0; i < array.length; i++) {\n");
            writer.write("            values[i] = array[i].getValue();\n");
            writer.write("        }\n");
            writer.write("        return values;\n");
            writer.write("    }\n");
            writer.write("    \n");
        }
        writer.write("}\n");
    }

    // Aliases (typedefs) don't exist in Java. We can emulate this using inheritance.
    // For primitives and Strings, we wrap the value.
    public boolean inherits() {
        return (! (type.isPrimitive
                || type.qualifiedJavaType.equals("java.lang.String")));
    }
}
