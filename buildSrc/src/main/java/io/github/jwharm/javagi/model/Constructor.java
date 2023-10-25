/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Constructor extends Method {

    public Constructor(GirElement parent, String name, String cIdentifier, String deprecated, String throws_) {
        super(parent, name, cIdentifier, deprecated, throws_, null, null, null);
    }

    public void generate(SourceWriter writer) throws IOException {
        String privateMethodName = "construct" + Conversions.toCamelCase(name, true);
        writer.write("\n");

        // Docs
        if (doc != null) {
            doc.generate(writer, false);
        }

        // @Deprecated
        if ("1".equals(deprecated)) {
            writer.write("@Deprecated\n");
        }

        // Name
        writer.write("public ");
        writer.write(((RegisteredType) parent).javaName);

        // Parameters
        writer.write("(");
        if (parameters != null) {
            parameters.generateJavaParameters(writer);
        }
        writer.write(")");

        // Throws
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }

        writer.write(" {\n");
        writer.increaseIndent();

        // Call super(constructNew())
        writer.write("super(" + privateMethodName + "(");
        if (parameters != null) {
            parameters.generateJavaParameterNames(writer);
        }
        writer.write("));\n");

        // Add instance to cache
        if (! (parent instanceof Record)) {
            writer.write("InstanceCache.put(handle(), this);\n");
        }

        writer.decreaseIndent();
        writer.write("}\n");

        // Generate constructor helper
        super.generate(writer);
    }

    public void generateNamed(SourceWriter writer) throws IOException {
        String privateMethodName = "construct" + Conversions.toCamelCase(name, true);
        RegisteredType constructed = (RegisteredType) parent;

        // Return value should always be the constructed type, but it is often specified in the GIR as
        // one of its parent types. For example, button#newWithLabel returns a Widget instead of a Button.
        // We override this for constructors, to always return the constructed type.
        returnValue.type = new Type(returnValue, constructed.name, constructed.cType + "*");
        returnValue.type.girElementInstance = constructed;
        returnValue.type.girElementType = constructed.getClass().getSimpleName();
        returnValue.type.init(constructed.name);

        writer.write("\n");

        // Docs
        if (doc != null) {
            doc.generate(writer, false);
        }

        // @Deprecated
        if ("1".equals(deprecated)) {
            writer.write("@Deprecated\n");
        }

        // Name
        writer.write("public static " + constructed.javaName + " " + Conversions.toLowerCaseJavaName(name));

        // Parameters
        writer.write("(");
        if (parameters != null) {
            parameters.generateJavaParameters(writer);
        }
        writer.write(")");

        // Throws
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }

        // Unsupported platforms
        if (doPlatformCheck() || parent.platforms.size() < 3) {
            writer.write((throws_ != null ? ", " : " throws ") + "UnsupportedPlatformException");
        }

        writer.write(" {\n");
        writer.increaseIndent();

        // Check for unsupported platforms
        generatePlatformCheck(writer);

        // Call native constructor function
        writer.write("var _result = " + privateMethodName + "(");
        if (parameters != null) {
            parameters.generateJavaParameterNames(writer);
        }
        writer.write(");\n");

        // Return statement
        returnValue.generateReturnStatement(writer);

        writer.decreaseIndent();
        writer.write("}\n");

        // Constructor helper
        super.generate(writer);
    }
}
