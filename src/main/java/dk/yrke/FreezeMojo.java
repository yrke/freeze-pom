package dk.yrke;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.dependency.fromDependencies.AbstractDependencyFilterMojo;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.dependency.utils.filters.ResolveFileFilter;
import org.apache.maven.plugins.dependency.utils.markers.SourcesFileMarkerHandler;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;


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

    @Parameter( property = "includeParents", defaultValue = "false" )
    boolean includeParents;

    @Override
    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new ResolveFileFilter( new SourcesFileMarkerHandler( this.markersDirectory ) );
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        DependencyStatusSets results = this.getDependencySets(false, includeParents);
        results.getResolvedDependencies().stream().sorted().forEach(it -> System.out.println(it));
    }
}