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
package org.spongepowered.gradle.vanilla;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.spongepowered.gradle.vanilla.model.DownloadClassifier;
import org.spongepowered.gradle.vanilla.model.Version;
import org.spongepowered.gradle.vanilla.model.VersionClassifier;
import org.spongepowered.gradle.vanilla.model.VersionDescriptor;
import org.spongepowered.gradle.vanilla.model.VersionManifestV2;
import org.spongepowered.gradle.vanilla.runs.RunConfiguration;
import org.spongepowered.gradle.vanilla.runs.RunConfigurationContainer;
import org.spongepowered.gradle.vanilla.task.AccessWidenJarTask;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import javax.inject.Inject;

public abstract class MinecraftExtension {

    private final Project project;
    private final Property<String> version;
    private final Property<MinecraftPlatform> platform;
    private final Property<Boolean> injectRepositories;
    private final VersionManifestV2 versionManifest;

    private final Provider<VersionDescriptor> versionDescriptor;
    private final Property<Version> targetVersion;

    private final DirectoryProperty assetsDirectory;
    private final DirectoryProperty remappedDirectory;
    private final DirectoryProperty originalDirectory;
    private final DirectoryProperty mappingsDirectory;
    private final DirectoryProperty filteredDirectory;
    private final DirectoryProperty decompiledDirectory;

    private final RunConfigurationContainer runConfigurations;
    private final Configuration minecraftClasspath;

    @Inject
    public MinecraftExtension(final Gradle gradle, final ObjectFactory factory, final Project project) throws IOException {
        this.project = project;
        this.versionManifest = VersionManifestV2.load();
        this.version = factory.property(String.class);
        this.platform = factory.property(MinecraftPlatform.class).convention(MinecraftPlatform.JOINED);
        this.injectRepositories = factory.property(Boolean.class).convention(true);
        this.assetsDirectory = factory.directoryProperty();
        this.remappedDirectory = factory.directoryProperty();
        this.originalDirectory = factory.directoryProperty();
        this.mappingsDirectory = factory.directoryProperty();
        this.filteredDirectory = factory.directoryProperty();
        this.decompiledDirectory = factory.directoryProperty();
        this.versionDescriptor = this.version.map(
            version -> this.versionManifest.findDescriptor(version)
                .orElseThrow(() -> new GradleException(String.format("Version '%s' specified in the 'minecraft' extension was not found in the "
                                + "manifest! Try '%s' instead.", this.version.get(), this.versionManifest.latest().get(VersionClassifier.RELEASE))))
        );
        this.targetVersion = factory.property(Version.class)
                .value(this.versionDescriptor.map(version -> {
            try {
                return version.toVersion();
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
        }));
        this.targetVersion.finalizeValueOnRead();

        this.runConfigurations = factory.newInstance(RunConfigurationContainer.class, factory.domainObjectContainer(RunConfiguration.class), this);

        final Path gradleHomeDirectory = gradle.getGradleUserHomeDir().toPath();
        final Path cacheDirectory = gradleHomeDirectory.resolve(Constants.Directories.CACHES);
        final Path rootDirectory = cacheDirectory.resolve(Constants.NAME);
        final Path jarsDirectory = rootDirectory.resolve(Constants.Directories.JARS);
        this.assetsDirectory.set(rootDirectory.resolve(Constants.Directories.ASSETS).toFile());
        this.remappedDirectory.set(jarsDirectory.resolve(Constants.Directories.REMAPPED).toFile());
        this.originalDirectory.set(jarsDirectory.resolve(Constants.Directories.ORIGINAL).toFile());
        this.mappingsDirectory.set(rootDirectory.resolve(Constants.Directories.MAPPINGS).toFile());
        this.filteredDirectory.set(jarsDirectory.resolve(Constants.Directories.FILTERED).toFile());
        this.decompiledDirectory.set(jarsDirectory.resolve(Constants.Directories.DECOMPILED).toFile());
        this.minecraftClasspath = project.getConfigurations().create(Constants.Configurations.MINECRAFT_CLASSPATH);
    }

    public Property<Boolean> injectRepositories() {
        return this.injectRepositories;
    }

    protected Property<String> version() {
        return this.version;
    }

    public void version(final String version) {
        this.version.set(version);
    }

    protected Property<MinecraftPlatform> platform() {
        return this.platform;
    }

    public void platform(final MinecraftPlatform platform) {
        this.platform.set(platform);
    }

    /**
     * Apply an access widener to the project.
     *
     * @param file any file that can be passed to {@link Project#file(Object)}
     */
    public void accessWidener(final Object file) {
        final TaskProvider<AccessWidenJarTask> awTask;
        final TaskContainer tasks = this.project.getTasks();
        if (tasks.getNames().contains(Constants.ACCESS_WIDENER_TASK_NAME)) {
            awTask = tasks.named(Constants.ACCESS_WIDENER_TASK_NAME, AccessWidenJarTask.class);
        } else {
            final Configuration accessWidener = this.project.getConfigurations().maybeCreate(Constants.Configurations.ACCESS_WIDENER);
            accessWidener.defaultDependencies(deps -> deps.add(this.project.getDependencies().create(Constants.WorkerDependencies.ACCESS_WIDENER)));
            final FileCollection widenerClasspath = accessWidener.getIncoming().getFiles();
            final Directory destinationDir = this.project.getLayout().getProjectDirectory().dir(".gradle").dir(Constants.NAME).dir("aw-minecraft");

            awTask = tasks.register(Constants.ACCESS_WIDENER_TASK_NAME, AccessWidenJarTask.class, task -> {
                task.setWorkerClasspath(widenerClasspath);
                task.getExpectedNamespace().set("named");
                task.getDestinationDirectory().set(destinationDir);
            });
        }

        awTask.configure(task -> task.getAccessWideners().from(file));
    }

    protected VersionManifestV2 versionManifest() {
        return this.versionManifest;
    }

    protected DirectoryProperty assetsDirectory() {
        return this.assetsDirectory;
    }

    protected DirectoryProperty remappedDirectory() {
        return this.remappedDirectory;
    }

    protected DirectoryProperty originalDirectory() {
        return this.originalDirectory;
    }

    protected DirectoryProperty mappingsDirectory() {
        return this.mappingsDirectory;
    }

    protected DirectoryProperty filteredDirectory() {
        return this.filteredDirectory;
    }

    protected DirectoryProperty decompiledDirectory() {
        return this.decompiledDirectory;
    }

    public RunConfigurationContainer getRuns() {
        return this.runConfigurations;
    }

    public void runs(final Action<RunConfigurationContainer> run) {
        Objects.requireNonNull(run, "run").execute(this.runConfigurations);
    }

    protected Provider<VersionDescriptor> versionDescriptor() {
        return this.versionDescriptor;
    }

    public Provider<Version> targetVersion() {
        return this.targetVersion;
    }

    protected void createMinecraftClasspath(final Project project) {
        this.minecraftClasspath.withDependencies(a -> {
            for (final MinecraftSide side : this.platform.get().activeSides()) {
                side.applyLibraries(
                    name -> a.add(project.getDependencies().create(name.group() + ':' + name.artifact() + ':' + name.version())),
                    this.targetVersion.get().libraries()
                );
            }
        });

        project.getConfigurations().named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).configure(path -> path.extendsFrom(this.minecraftClasspath));
        project.getConfigurations().named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).configure(path -> path.extendsFrom(this.minecraftClasspath));
    }

    Configuration minecraftClasspathConfiguration() {
        return this.minecraftClasspath;
    }
}
