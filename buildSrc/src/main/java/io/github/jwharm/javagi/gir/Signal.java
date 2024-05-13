/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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

package io.github.jwharm.javagi.gir;

import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;

import static io.github.jwharm.javagi.util.Conversions.toJavaSimpleType;

public final class Signal extends Multiplatform implements Callable {

    public Signal(Map<String, String> attributes,
                  List<Node> children,
                  int platforms) {
        super(attributes, children, platforms);
    }

    @Override
    public RegisteredType parent() {
        return (RegisteredType) super.parent();
    }

    public TypeName typeName() {
        return parent().typeName().nestedClass(
                toJavaSimpleType(name() + "_callback", namespace()));
    }

    public boolean detailed() {
        return attrBool("detailed", false);
    }

    public When when() {
        return When.from(attr("when"));
    }

    public boolean action() {
        return attrBool("action", false);
    }

    public boolean noHooks() {
        return attrBool("no-hooks", false);
    }

    public boolean noRecurse() {
        return attrBool("no-recurse", false);
    }

    public String emitter() {
        return attr("emitter");
    }
}
