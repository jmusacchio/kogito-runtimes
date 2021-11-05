/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.incubation.common;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An empty DataContext singleton
 */
@JsonSerialize // ensure Jackson won't complain even if it is an empty object
public final class EmptyMetaDataContext implements MetaDataContext {
    public static final MetaDataContext Instance = new EmptyMetaDataContext();

    private EmptyMetaDataContext() {
    }
}