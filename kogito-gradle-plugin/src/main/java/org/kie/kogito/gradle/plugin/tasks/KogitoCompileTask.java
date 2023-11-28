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

import javax.inject.Inject;

import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.kie.kogito.gradle.plugin.extensions.KogitoExtension;
import org.kie.kogito.gradle.plugin.util.Util;

import static org.kie.kogito.gradle.plugin.util.Util.projectSourceDirectory;

@CacheableTask
public class KogitoCompileTask extends JavaCompile {

    @org.gradle.api.tasks.Optional
    @InputFiles
    @CompileClasspath
    private File generatedSources;

    @org.gradle.api.tasks.Optional
    @InputFiles
    @CompileClasspath
    private File generatedResources;

    @Inject
    public KogitoCompileTask(KogitoExtension extension, AbstractCompile compile) {
        super();
        this.generatedSources = extension.getGeneratedSources();
        this.generatedResources = extension.getGeneratedResources();
        source(
                getGeneratedSources(), getGeneratedResources(), projectSourceDirectory(this.getProject())
                        .getSrcDirs()
                        .stream()
                        .findFirst()
                        .orElse(getGeneratedSources()));
        setClasspath(compile.getClasspath());
        setDestinationDir(compile.getDestinationDir());
    }

    @Override
    public FileCollection getClasspath() {
        return getServices().get(FileCollectionFactory.class).fixed(Util.classpathFiles(getProject()));
    }

    public File getGeneratedSources() {
        return generatedSources;
    }

    public File getGeneratedResources() {
        return generatedResources;
    }
}
