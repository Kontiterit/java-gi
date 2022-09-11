package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Method extends GirElement implements CallableType {

    public final String cIdentifier, deprecated, throws_;
    public ReturnValue returnValue;
    public Parameters parameters;

    public Method(GirElement parent, String name, String cIdentifier, String deprecated, String throws_) {
        super(parent);
        this.name = name;
        this.cIdentifier = cIdentifier;
        this.deprecated = deprecated;
        this.throws_ = throws_;

        // Handle empty names. (For example, GLib.g_iconv is named "".)
        if ("".equals(name)) {
            this.name = cIdentifier;
        }
    }

    public void generate(Writer writer, boolean isDefault, boolean isStatic) throws IOException {

        // Do not generate deprecated methods.
        if ("1".equals(deprecated)) {
            return;
        }

        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                if (p.type != null && p.type.isCallback()) {
                    generateCallbackMethod(writer, (Callback) p.type.girElementInstance, p.type.simpleJavaType, p.name, isDefault, isStatic);
                    return;
                }
            }
        }

        writeMethodDeclaration(writer, doc, name, throws_, isDefault, isStatic);
        writer.write(" {\n");

        if (throws_ != null) {
            writer.write("        MemorySegment GERROR = Interop.getAllocator().allocate(ValueLayout.ADDRESS);\n");
        }
        writer.write("        ");
        if (! returnValue.type.isVoid()) {
            writer.write("var RESULT = ");
        }
        writer.write("gtk_h." + cIdentifier);
        if (parameters != null) {
            writer.write("(");
            parameters.generateCParameters(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(";\n");

        if (throws_ != null) {
            writer.write("        if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write("            throw new GErrorException(GERROR);\n");
            writer.write("        }\n");
        }
        returnValue.generateReturnStatement(writer, 2);

        writer.write("    }\n");
        writer.write("    \n");
    }

    private void generateCallbackMethod(Writer writer, Callback callback, String callbackType, String callbackParameterName, boolean isDefault, boolean isStatic) throws IOException {
        if (doc != null) {
            doc.generate(writer, 1);
        }

        String methodName = Conversions.toLowerCaseJavaName(name);
        if (isDefault) { // Overriding toString() in a default method is not allowed.
            methodName = Conversions.replaceJavaObjectMethodNames(methodName);
        }

        writer.write("    public " + (isDefault ? "default " : "") + "void " + methodName + "(");

        // Loop through all parameters except the last (the userdata pointer).
        for (int p = 0; p < parameters.parameterList.size() - 1; p++) {
            Parameter parameter = parameters.parameterList.get(p);
            if (parameter instanceof InstanceParameter) {
                continue;
            }
            parameter.generateTypeAndName(writer);
            if (p < parameters.parameterList.size() - 2) {
                writer.write(", ");
            }
        }
        writer.write(") {\n");
        writer.write("        try {\n");
        writer.write("            int hash = " + callbackParameterName + ".hashCode();\n");
        writer.write("            JVMCallbacks.signalRegistry.put(hash, " + callbackParameterName + ");\n");
        writer.write("            MemorySegment intSegment = Interop.getAllocator().allocate(C_INT, hash);\n");
        writer.write("            MethodType methodType = MethodType.methodType(");

        writer.write("void.class");
        if (callback.parameters != null) {
            for (Parameter p : callback.parameters.parameterList) {
                writer.write(", " + Conversions.toPanamaJavaType(p.type) + ".class");
            }
        }
        writer.write(");\n");

        writer.write("            MethodHandle methodHandle = MethodHandles.lookup().findStatic(JVMCallbacks.class, \"cb" + callbackType + "\", methodType);\n");
        writer.write("            FunctionDescriptor descriptor = FunctionDescriptor.");

        writer.write("ofVoid(");
        if (callback.parameters != null) {
            for (int p = 0; p < callback.parameters.parameterList.size(); p++) {
                if (p > 0) {
                    writer.write(", ");
                }
                writer.write(Conversions.toPanamaMemoryLayout(callback.parameters.parameterList.get(p).type));
            }
        }
        writer.write(");\n");

        writer.write("            NativeSymbol nativeSymbol = CLinker.systemCLinker().upcallStub(methodHandle, descriptor, Interop.getScope());\n");
        writer.write("            gtk_h." + cIdentifier + "(");
        for (int p = 0; p < parameters.parameterList.size(); p++) {
            if (p > 0) {
                writer.write(", ");
            }
            Parameter parameter = parameters.parameterList.get(p);
            if (parameter instanceof InstanceParameter) {
                writer.write("handle()");
            } else if (parameter.type != null && parameter.type.isCallback()) {
                writer.write("nativeSymbol");
            } else if (p == parameters.parameterList.size() - 1) {
                writer.write("intSegment");
            } else {
                parameter.generateInterop(writer);
            }
        }
        writer.write(");\n");

        // NoSuchMethodException, IllegalAccessException from findStatic()
        // When the static callback methods have been successfully generated, these exceptions should never happen.
        // We can try to suppress them, but I think it's better to be upfront when they occur, and just crash
        // immediately so the stack trace will be helpful to solve the issue.
        writer.write("        } catch (Exception e) {\n");
        writer.write("            throw new RuntimeException(e);\n");
        writer.write("        }\n");
        writer.write("    }\n");
        writer.write("    \n");
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Parameters ps) {
        this.parameters = ps;
    }

    @Override
    public ReturnValue getReturnValue() {
        return returnValue;
    }

    @Override
    public void setReturnValue(ReturnValue rv) {
        this.returnValue = rv;
    }
}
