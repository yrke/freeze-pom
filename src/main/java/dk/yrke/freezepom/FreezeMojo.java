package dk.yrke.freezepom;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.dependency.fromDependencies.AbstractDependencyFilterMojo;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.dependency.utils.filters.ResolveFileFilter;
import org.apache.maven.plugins.dependency.utils.markers.SourcesFileMarkerHandler;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Mojo( name = "freeze", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
class FreezeMojo extends AbstractDependencyFilterMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    // XXX name project will conflict with AbstractDependencyFilterMojo
    private MavenProject project2;

    @Parameter( property = "includeParents", defaultValue = "false" )
    boolean includeParents;

    @Override
    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new ResolveFileFilter( new SourcesFileMarkerHandler( this.markersDirectory ) );
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {

        var projectDir = project2.getBasedir();

        DependencyStatusSets results = this.getDependencySets(false, includeParents);

        var resolvedDepends = results.getResolvedDependencies();

        if (resolvedDepends != null && !resolvedDepends.isEmpty()) {
            try (PrintStream s = new PrintStream(new File(projectDir, Settings.FREEZE_FILE_NAME), StandardCharsets.UTF_8)) {
                s.print(resolvedDepends.stream().map(it -> it.getId()).sorted().collect(Collectors.joining("\n")));
            } catch (IOException e) {
                throw new RuntimeException("Failure writing freeze file", e);
            }
        } else {
            throw new RuntimeException("Not found any dependencies");
        }

    }
}