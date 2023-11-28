/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.gradle.plugin.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.drools.codegen.common.GeneratedFile;
import org.drools.codegen.common.GeneratedFileType;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.kie.kogito.codegen.core.ApplicationGenerator;
import org.kie.kogito.codegen.core.utils.ApplicationGeneratorDiscovery;
import org.kie.kogito.gradle.plugin.extensions.GenerateModelExtension;
import org.kie.kogito.gradle.plugin.extensions.KogitoExtension;

import static org.drools.codegen.common.GeneratedFileType.COMPILED_CLASS;
import static org.kie.efesto.common.api.constants.Constants.INDEXFILE_DIRECTORY_PROPERTY;
import static org.kie.kogito.gradle.plugin.util.Util.projectSourceDirectory;

@CacheableTask
public class GenerateModelTask extends AbstractKieTask {
    public static final PathMatcher drlFileMatcher = FileSystems.getDefault().getPathMatcher("glob:**.drl");

    @org.gradle.api.tasks.Optional
    @InputFiles
    @CompileClasspath
    private File customizableSourcesPath;

    @org.gradle.api.tasks.Optional
    @Input
    private Boolean generatePartial;

    @org.gradle.api.tasks.Optional
    @Input
    private Boolean onDemand;

    @org.gradle.api.tasks.Optional
    @Input
    private Boolean keepSources;

    @org.gradle.api.tasks.Optional
    @Input
    private String buildOutputDirectory;

    @Inject
    public GenerateModelTask(KogitoExtension extension, GenerateModelExtension modelExtension) {
        super(extension);
        this.customizableSourcesPath = modelExtension.getCustomizableSourcesPath();
        this.generatePartial = modelExtension.isGeneratePartial();
        this.onDemand = modelExtension.isOnDemand();
        this.keepSources = modelExtension.isKeepSources();
        this.buildOutputDirectory = modelExtension.getBuildOutputDirectory();
    }

    @TaskAction
    public void execute() {
        // TODO to be removed with DROOLS-7090
        boolean indexFileDirectorySet = false;
        this.getLogger().debug("execute -> " + getBuildOutputDirectory());
        if (getBuildOutputDirectory() == null) {
            throw new RuntimeException("${project.buildDir} is null");
        } else {
            if (System.getProperty(INDEXFILE_DIRECTORY_PROPERTY) == null) {
                System.setProperty(INDEXFILE_DIRECTORY_PROPERTY, getBuildOutputDirectory());
                indexFileDirectorySet = true;
            }

            this.addCompileSourceRoots();

            if (getOnDemand()) {
                this.getLogger().info("On-Demand Mode is On. Use gradle build :scaffold");
            } else {
                this.generateModel();
            }

            if (indexFileDirectorySet) {
                System.clearProperty(INDEXFILE_DIRECTORY_PROPERTY);
            }
        }
    }

    protected void addCompileSourceRoots() {
        projectSourceDirectory(this.getProject())
                .srcDirs(
                        getCustomizableSourcesPath().getPath(),
                        getGeneratedSources().getPath());
    }

    protected void generateModel() {
        this.setSystemProperties(getProperties());
        ApplicationGenerator appGen = ApplicationGeneratorDiscovery.discover(this.discoverKogitoRuntimeContext(this.projectClassLoader()));
        Collection<GeneratedFile> generatedFiles;
        if (getGeneratePartial()) {
            generatedFiles = appGen.generateComponents();
        } else {
            generatedFiles = appGen.generate();
        }

        Map<GeneratedFileType, List<GeneratedFile>> mappedGeneratedFiles = generatedFiles.stream()
                .collect(Collectors.groupingBy(GeneratedFile::type));
        mappedGeneratedFiles.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(COMPILED_CLASS))
                .forEach(entry -> writeGeneratedFiles(entry.getValue()));

        List<GeneratedFile> generatedCompiledFiles = mappedGeneratedFiles.getOrDefault(COMPILED_CLASS,
                Collections.emptyList())
                .stream().map(originalGeneratedFile -> new GeneratedFile(COMPILED_CLASS, convertPath(originalGeneratedFile.path().toString()), originalGeneratedFile.contents()))
                .collect(Collectors.toList());

        writeGeneratedFiles(generatedCompiledFiles);

        if (!getKeepSources()) {
            this.deleteDrlFiles();
        }
    }

    private String convertPath(String toConvert) {
        return toConvert.replace('.', File.separatorChar) + ".class";
    }

    private void deleteDrlFiles() {
        // Remove drl files
        try (final Stream<Path> drlFiles = Files.find(getOutputDirectory().toPath(), Integer.MAX_VALUE,
                (p, f) -> drlFileMatcher.matches(p))) {
            drlFiles.forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to find .drl files");
        }
    }

    public File getCustomizableSourcesPath() {
        return customizableSourcesPath;
    }

    public Boolean getGeneratePartial() {
        return generatePartial;
    }

    public Boolean getOnDemand() {
        return onDemand;
    }

    public void setOnDemand(Boolean onDemand) {
        this.onDemand = onDemand;
    }

    public Boolean getKeepSources() {
        return keepSources;
    }

    public String getBuildOutputDirectory() {
        return buildOutputDirectory;
    }
}
