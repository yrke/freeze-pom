package dk.yrke

import org.apache.commons.lang3.StringUtils
import org.apache.maven.RepositoryUtils
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Model
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.DefaultProjectBuildingRequest
import org.apache.maven.project.MavenProject
import org.apache.maven.project.ProjectBuilder
import org.apache.maven.project.ProjectBuildingRequest
import org.eclipse.aether.RepositoryException
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResult
import java.io.File
import java.io.FileWriter
import java.io.IOException


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
 */ /**
 * Goal which touches a timestamp file.
 *
 * @goal touch
 *
 * @phase process-sources
 */
@Mojo( name = "freeze", aggregator = true )
class FreezeMojo : AbstractMojo() {
    /**
     * Location of the file.
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private val outputDirectory: File? = null

    /**
     * The current build session instance. This is used for
     * plugin manager API calls.
     */
    @Parameter(defaultValue = "\${session}", readonly = true, required = true)
    protected var session: MavenSession? = null

    @Parameter(property = "artifact")
    private val artifact: String? = null

    @Parameter(defaultValue = "\${project.remoteArtifactRepositories}", required = true, readonly = true)
    protected var remoteRepositories: List<ArtifactRepository>? = null

    @Parameter(defaultValue = "\${localRepository}", required = true, readonly = true)
    protected var localRepository: ArtifactRepository? = null

    @Parameter(defaultValue = "\${project.pluginArtifactRepositories}", required = true, readonly = true)
    protected var pluginArtifactRepositories: List<ArtifactRepository>? = null

    @Component
    protected var projectBuilder: ProjectBuilder? = null

    /**
     * Retrieves the Maven Project associated with the given artifact String, in the form of
     * `groupId:artifactId[:version]`. This resolves the POM artifact at those coordinates and then builds
     * the Maven project from it.
     *
     * @param artifactString Coordinates of the Maven project to get.
     * @return New Maven project.
     * @throws MojoExecutionException If there was an error while getting the Maven project.
     */
    @Throws(MojoExecutionException::class)
    protected fun getMavenProject(artifactString: String): MavenProject? {
        return try {
            val pbr: ProjectBuildingRequest = DefaultProjectBuildingRequest(session?.getProjectBuildingRequest())
            pbr.setRemoteRepositories(remoteRepositories)
            pbr.setLocalRepository(localRepository)
            pbr.setPluginArtifactRepositories(pluginArtifactRepositories)
            pbr.setProject(null)
            pbr.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
            pbr.setResolveDependencies(true)
            val artifact: Artifact = resolveArtifact(
                getAetherArtifact(artifactString, "pom")
            )!!.getArtifact()
            projectBuilder?.build(artifact.getFile(), pbr)!!.getProject()
        } catch (e: Exception) {
            throw MojoExecutionException(
                "Unable to get the POM for the artifact '" + artifactString
                        + "'. Verify the artifact parameter.", e
            )
        }
    }

    @Component
    protected var repositorySystem: RepositorySystem? = null
    @Throws(RepositoryException::class)
    protected fun resolveArtifact(
        artifact: Artifact?
    ): ArtifactResult? {
        val repositories = RepositoryUtils.toRepos(remoteRepositories)
        val repositorySession = session!!.projectBuildingRequest.repositorySession

        // use descriptor to respect relocation
        val artifactDescriptor: ArtifactDescriptorResult = repositorySystem!!.readArtifactDescriptor(
            repositorySession, ArtifactDescriptorRequest(artifact, repositories, null)
        )
        return repositorySystem!!.resolveArtifact(
            repositorySession,
            ArtifactRequest(artifactDescriptor.artifact, repositories, null)
        )
    }

    @Throws(MojoExecutionException::class)
    protected fun getAetherArtifact(artifactString: String, type: String?): Artifact? {
        require(!StringUtils.isEmpty(artifactString)) { "artifact parameter could not be empty" }
        val groupId: String // required
        val artifactId: String // required
        val version: String // optional
        val artifactParts = artifactString.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        when (artifactParts.size) {
            2 -> {
                groupId = artifactParts[0]
                artifactId = artifactParts[1]
                version = org.apache.maven.artifact.Artifact.LATEST_VERSION
            }

            3 -> {
                groupId = artifactParts[0]
                artifactId = artifactParts[1]
                version = artifactParts[2]
            }

            else -> throw MojoExecutionException(
                "The artifact parameter '" + artifactString
                        + "' should be conform to: " + "'groupId:artifactId[:version]'."
            )
        }
        return DefaultArtifact(groupId, artifactId, type, version)
    }

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private var project: MavenProject? = null

    @Throws(MojoExecutionException::class)
    override fun execute() {

        if ( StringUtils.isNotEmpty( artifact ) )
        {
            project = getMavenProject( artifact!! );
        }

        val pom: Model = project!!.getModel()

        pom.dependencies.forEach{
            println("${it.groupId}:${it.artifactId}:${it.version}:${it.scope}")
        }

        //cleanModel(pom)
        //val properties: Properties = SortedProperties()
        //properties.putAll(pom.properties)
        //pom.properties = properties

    }


}