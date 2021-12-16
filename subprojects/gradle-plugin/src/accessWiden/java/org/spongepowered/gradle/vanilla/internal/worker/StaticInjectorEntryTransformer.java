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
package org.spongepowered.gradle.vanilla.internal.worker;

import com.maldloader.staticinjector.StaticInjector;
import net.minecraftforge.fart.api.Transformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.List;

final class StaticInjectorEntryTransformer implements Transformer {
    private final StaticInjector staticInjector;

    public StaticInjectorEntryTransformer(final StaticInjector staticInjector) {
        this.staticInjector = staticInjector;
    }

    @Override
    public ClassEntry process(final ClassEntry entry) {
        // Because InnerClass attributes can be present in any class SI'd classes
        // are referenced from, we have to target every class to get a correct output.
        final ClassReader reader = new ClassReader(entry.getData());
        final ClassWriter writer = new ClassWriter(reader, 0);
        // TODO: Expose the ASM version constant somewhere visible to this worker
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                List<String> newInterfaces = staticInjector.getInterfaces(name);
                if(newInterfaces != null) {
                    int oldLength = interfaces.length;
                    interfaces = Arrays.copyOf(interfaces, interfaces.length + newInterfaces.size());
                    StringBuilder signatureBuilder = new StringBuilder(signature);
                    for (int i = 0; i < newInterfaces.size(); i++) {
                        interfaces[oldLength + i] = newInterfaces.get(i);
                        signatureBuilder.append("L").append(newInterfaces.get(i)).append(";");
                    }
                    signature = signatureBuilder.toString();
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
        }, 0);
        return ClassEntry.create(entry.getName(), entry.getTime(), writer.toByteArray());
    }
}
