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

package io.github.jwharm.javagi.test.gio;

import org.gnome.gio.SimpleAction;
import org.gnome.gobject.WeakRef;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Create a GWeakRef to a GObject and read it back.
 */
public class WeakRefTest {

    @Test
    public void createWeakRef() {
        SimpleAction gobject = new SimpleAction("test", null);

        @SuppressWarnings("unchecked")
        WeakRef<SimpleAction> weakRef = WeakRef.allocate(Arena.ofAuto());
        weakRef.init(gobject);

        SimpleAction action2 = weakRef.get();
        weakRef.clear();

        assertEquals(gobject, action2);
    }
}
