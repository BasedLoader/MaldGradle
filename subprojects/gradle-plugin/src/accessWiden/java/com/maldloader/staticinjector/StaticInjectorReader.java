/*
 * This file is part of VanillaGradle, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.maldloader.staticinjector;

import java.io.BufferedReader;
import java.io.IOException;

public final class StaticInjectorReader {
    private final StaticInjector staticInjector;

    public StaticInjectorReader(StaticInjector staticInjector) {
        this.staticInjector = staticInjector;
    }

    public void read(BufferedReader reader) throws IOException {
        this.read(reader, null);
    }

    public void read(BufferedReader reader, String currentNamespace) throws IOException {
        String[] header = reader.readLine().split("\\s+");

        if (header.length != 3 || !header[0].equals("staticInjector")) {
            throw new UnsupportedOperationException("Invalid static injector file");
        }

        if (!header[1].equals("v1")) {
            throw new RuntimeException(String.format("Unsupported static injector format (%s)", header[1]));
        }

        if (currentNamespace != null && !header[2].equals(currentNamespace)) {
            throw new RuntimeException(String.format("Namespace (%s) does not match current runtime namespace (%s)", header[2], currentNamespace));
        }

        if (staticInjector.namespace != null) {
            if (!staticInjector.namespace.equals(header[2])) {
                throw new RuntimeException(String.format("Namespace mismatch, expected %s got %s", staticInjector.namespace, header[2]));
            }
        }

        staticInjector.namespace = header[2];

        String line;
        while ((line = reader.readLine()) != null) {
            //Comment handling
            int commentPos = line.indexOf('#');

            if (commentPos >= 0) {
                line = line.substring(0, commentPos).trim();
            }

            if (line.isEmpty()) continue;

            String[] split = line.split("\\s+");

            if (split.length != 3) {
                throw new RuntimeException(String.format("Invalid line (%s)", line));
            }

            String side = split[0];
            String mcClass = split[1];
            String injectingInterface = split[2];
            staticInjector.addOrSet(side, mcClass, injectingInterface);
        }
    }
}
