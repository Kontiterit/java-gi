package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Constructor extends Method {

    public Constructor(GirElement parent, String name, String cIdentifier, String deprecated, String throws_) {
        super(parent, name, cIdentifier, deprecated, throws_);
    }

    public void generate(Writer writer) throws IOException {
        // Do not generate deprecated constructors.
        if ("1".equals(deprecated)) {
            return;
        }

        generateMethodHandle(writer);

        String privateMethodName = generateConstructorHelper(writer);

        if (doc != null) {
            doc.generate(writer, 1);
        }
        writer.write("    public ");
        writer.write(((RegisteredType) parent).javaName);
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameters(writer, false);
            writer.write(")");
        } else {
            writer.write("()");
        }
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }
        writer.write(" {\n");
        writer.write("        super(" + privateMethodName);
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameterNames(writer);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(");\n");
        writer.write("    }\n");
        writer.write("    \n");
    }

    public void generateNamed(Writer writer) throws IOException {
        RegisteredType clazz = (RegisteredType) parent;

        generateMethodHandle(writer);

        String privateMethodName = generateConstructorHelper(writer);

        if (doc != null) {
            doc.generate(writer, 1);
        }
        writer.write("    public static " + clazz.javaName + " " + Conversions.toLowerCaseJavaName(name));
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameters(writer, false);
            writer.write(")");
        } else {
            writer.write("()");
        }
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }
        writer.write(" {\n");
        writer.write("        return new " + clazz.javaName + "(" + privateMethodName);
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameterNames(writer);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(");\n");
        writer.write("    }\n");
        writer.write("    \n");
    }

    // Because constructors sometimes throw exceptions, we need to allocate a GError segment before
    // calling "super(gtk_h.ns_obj_new(..., GERROR))", which is not allowed - the super() call must
    // be the first statement in the constructor. Therefore, we always generate a private method that
    // prepares a GError memorysegment if necessary, calls the C API and throws the GErrorException
    // (if necessary). The "real" constructor just calls super(private_method());
    private String generateConstructorHelper(Writer writer) throws IOException {
        String methodName = "construct" + Conversions.toCamelCase(name, true);
        writer.write("    private static Refcounted " + methodName);
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameters(writer, false);
            writer.write(")");
        } else {
            writer.write("()");
        }
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }
        writer.write(" {\n");
        if (throws_ != null) {
            writer.write("        MemorySegment GERROR = Interop.getAllocator().allocate(ValueLayout.ADDRESS);\n");
        }
        writer.write("        try {\n");
        writer.write("            Refcounted RESULT = Refcounted.get((MemoryAddress) " + cIdentifier + ".invokeExact");
        if (parameters != null) {
            writer.write("(");
            parameters.generateCParameters(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(returnValue.transferOwnership() ? ", true" : ", false");
        writer.write(");\n");
        if (throws_ != null) {
            writer.write("            if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write("                throw new GErrorException(GERROR);\n");
            writer.write("            }\n");
        }
        writer.write("            return RESULT;\n");
        writer.write("        } catch (Throwable ERR) {\n");
        writer.write("            throw new AssertionError(\"Unexpected exception occured: \", ERR);\n");
        writer.write("        }\n");
        writer.write("    }\n");
        writer.write("    \n");
        return methodName;
    }
}