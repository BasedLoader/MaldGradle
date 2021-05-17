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
package org.spongepowered.gradle.vanilla.internal.model;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.net.URL;

/**
 * A single file download in the version manifest.
 */
@Value.Immutable
@Gson.TypeAdapters
public interface Download {

    /**
     * Path identifying the standard destination for this download.
     *
     * <p>Not always included.</p>
     *
     * @return the download path
     */
    @Nullable String path();

    /**
     * Get the SHA-1 hash expected for the download.
     *
     * @return the hash
     */
    String sha1();

    /**
     * Expected size of the download, in bytes.
     *
     * @return the file size
     */
    int size();

    /**
     * The URL to download from.
     *
     * @return file download URL
     */
    URL url();

}
