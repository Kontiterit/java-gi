package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.StringWriter;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

public class Method extends GirElement implements CallableType {

    public final String cIdentifier;
    public final String deprecated;
    public final String throws_;
    public final String shadowedBy;
    public final String shadows;
    public final String movedTo;

    public ReturnValue returnValue;
    public Parameters parameters;

    private boolean skip = false;

    public Method(GirElement parent, String name, String cIdentifier, String deprecated,
                  String throws_, String shadowedBy, String shadows, String movedTo) {
        super(parent);
        this.name = shadows == null ? name : shadows; // Language bindings are expected to rename a function to the shadowed function
        this.cIdentifier = cIdentifier;
        this.deprecated = deprecated;
        this.throws_ = throws_;
        this.shadowedBy = shadowedBy;
        this.shadows = shadows;
        this.movedTo = movedTo;
        
        // Generated under another name
        if (shadowedBy != null)
            this.skip = true;

        // Rename methods with "moved-to" attribute, but skip generation if the "moved-to" name has
        // the form of "Type.new_name", because in that case it already exists under the new name.
        if (movedTo != null) {
            if (movedTo.contains("."))
                this.skip = true;
            else
                this.name = movedTo;
        }
    }
    
    protected boolean generateFunctionDescriptor(SourceWriter writer) throws IOException {
        boolean varargs = false;
        
        writer.write("FunctionDescriptor.");
        if (returnValue.type != null && "void".equals(returnValue.type.simpleJavaType)) {
            writer.write("ofVoid(");
        } else {
            writer.write("of(" + Conversions.toPanamaMemoryLayout(returnValue.type));
            if (parameters != null) {
                writer.write(", ");
            }
        }
        if (parameters != null) {
            for (int i = 0; i < parameters.parameterList.size(); i++) {
                if (parameters.parameterList.get(i).varargs) {
                    varargs = true;
                    break;
                }
                if (i > 0) {
                    writer.write(", ");
                }
                writer.write(Conversions.toPanamaMemoryLayout(parameters.parameterList.get(i).type));
            }
        }
        if (throws_ != null) {
            writer.write(", Interop.valueLayout.ADDRESS");
        }
        
        writer.write(")");
        
        return varargs;
    }
    
    public void generateMethodHandle(SourceWriter writer) throws IOException {
        if (skip) return;

        boolean varargs = false;
        writer.write("\n");
        writer.write(parent instanceof Interface ? "@ApiStatus.Internal\n" : "private ");
        writer.write("static final MethodHandle " + cIdentifier + " = Interop.downcallHandle(\n");
        writer.write("        \"" + cIdentifier + "\",\n");
        writer.write("        ");
        varargs = generateFunctionDescriptor(writer);
        writer.write(",\n");
        writer.write(varargs ? "        true\n" : "        false\n");
        writer.write(");\n");
    }
    
    protected String getMethodDeclaration() throws IOException {
        SourceWriter writer = new SourceWriter(new StringWriter());
        
        if ((parent instanceof Interface) && (! (this instanceof Function))) {
            // Default interface methods
            writer.write("default ");
        } else {
            // Visibility
            writer.write("public ");
        }

        // Static methods (functions)
        if (this instanceof Function) {
            writer.write("static ");
        }

        // Return type
        getReturnValue().writeType(writer, true, true);

        // Method name
        String methodName = Conversions.toLowerCaseJavaName(name);
        if (parent instanceof Interface) { // Overriding toString() in a default method is not allowed.
            methodName = Conversions.replaceJavaObjectMethodNames(methodName);
        }
        writer.write(" ");
        writer.write(methodName);

        // Parameters
        if (getParameters() != null) {
            writer.write("(");
            getParameters().generateJavaParameters(writer, false);
            writer.write(")");
        } else {
            writer.write("()");
        }

        // Exceptions
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }
        
        return writer.toString();
    }

    protected String getMethodSpecification() throws IOException {
        SourceWriter writer = new SourceWriter(new StringWriter());
        
        // Method name
        String methodName = Conversions.toLowerCaseJavaName(name);
        if (parent instanceof Interface) { // Overriding toString() in a default method is not allowed.
            methodName = Conversions.replaceJavaObjectMethodNames(methodName);
        }
        writer.write(methodName);

        // Parameters
        if (getParameters() != null) {
            writer.write("(");
            getParameters().generateJavaParameterTypes(writer, false, false);
            writer.write(")");
        } else {
            writer.write("()");
        }

        // Exceptions
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }
        
        return writer.toString();
    }

    public void generate(SourceWriter writer) throws IOException {
        if (skip) return;

        writer.write("\n");
        
        // Documentation
        if (doc != null) {
            doc.generate(writer, false);
        }

        // Deprecation
        if ("1".equals(deprecated)) {
            writer.write("@Deprecated\n");
        }

        // Declaration
        writer.write(getMethodDeclaration());
        
        writer.write(" {\n");
        writer.increaseIndent();

        // Generate try-with-resources?
        boolean hasScope = allocatesMemory();
        if (hasScope) {
            writer.write("try (MemorySession SCOPE = MemorySession.openConfined()) {\n");
            writer.increaseIndent();
        }

        // Generate preprocessing statements for all parameters
        if (parameters != null) {
            parameters.generatePreprocessing(writer);
        }

        // Allocate GError pointer
        if (throws_ != null) {
            writer.write("MemorySegment GERROR = SCOPE.allocate(Interop.valueLayout.ADDRESS);\n");
        }
        
        // Variable declaration for return value
        String panamaReturnType = Conversions.toPanamaJavaType(getReturnValue().type);
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write(panamaReturnType + " RESULT;\n");
        }
        
        // The method call is wrapped in a try-catch block
        writer.write("try {\n");
        writer.increaseIndent();

        // Generate the return type
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("RESULT = (");
            writer.write(panamaReturnType);
            writer.write(") ");
        }

        // Invoke to the method handle
        writer.write("DowncallHandles." + cIdentifier + ".invokeExact");
        
        // Marshall the parameters to the native types
        if (parameters != null) {
            writer.write("(");
            parameters.marshalJavaToNative(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(";\n");
        
        // If something goes wrong in the invokeExact() call
        writer.decreaseIndent();
        writer.write("} catch (Throwable ERR) {\n");
        writer.write("    throw new AssertionError(\"Unexpected exception occured: \", ERR);\n");
        writer.write("}\n");

        // Throw GErrorException
        if (throws_ != null) {
            writer.write("if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write("    throw new GErrorException(GERROR);\n");
            writer.write("}\n");
        }
        
        // Generate post-processing actions for parameters
        if (parameters != null) {
            parameters.generatePostprocessing(writer);
        }

        // Generate a call to "yieldOwnership" to disable the Cleaner
        // when a user manually calls "unref"
        if ((! (this instanceof Function)) && name.equals("unref")) {
            writer.write("this.yieldOwnership();\n");
        }
        
        // Generate code to process and return the result value
        returnValue.generate(writer, panamaReturnType);

        // End of memory allocation scope
        if (hasScope) {
            writer.decreaseIndent();
            writer.write("}\n");
        }

        writer.decreaseIndent();
        writer.write("}\n");
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

    @Override
    public Doc getDoc() {
        return doc;
    }

    @Override
    public String getThrows() {
        return throws_;
    }
}
