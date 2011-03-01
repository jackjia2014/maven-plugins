package org.apache.maven.plugins.site;

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


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Deploys the generated site to a staging or mock directory to the site URL
 * specified in the <code>&lt;distributionManagement&gt;</code> section of the
 * POM. It supports <code>scp</code> and <code>file</code> protocols for
 * deployment.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal stage-deploy
 * @requiresDependencyResolution test
 */
public class SiteStageDeployMojo
    extends AbstractDeployMojo
{
    /**
     * The staged site will be deployed to this URL.
     *
     * If you don't specify this, the default-value will be
     * "${project.distributionManagement.site.url}/staging", where "project" is
     * either the current project or, in a reactor build, the top level project
     * in the reactor.
     * <p>
     * Note that even if you specify this plugin parameter you still need to indicate
     * ${project.distributionManagement.site.url} at least in your top level project
     * in order for relative links between modules to be resolved correctly.
     * </p>
     *
     * @parameter expression="${stagingSiteURL}"
     * @see <a href="http://maven.apache.org/maven-model/maven.html#class_site">MavenModel#class_site</a>
     */
    private String stagingSiteURL;

    /**
     * The identifier of the repository where the staging site will be deployed. This id will be used to lookup a
     * corresponding <code>&lt;server&gt;</code> entry from the <code>settings.xml</code>. If a matching
     * <code>&lt;server&gt;</code> entry is found, its configured credentials will be used for authentication.
     *
     * @parameter expression="${stagingRepositoryId}" default-value="stagingSite"
     * @since 2.0.1
     */
    private String stagingRepositoryId;

    private static final String DEFAULT_STAGING_DIRECTORY = "staging";


    @Override
    protected String getDeployRepositoryID()
        throws MojoExecutionException
    {
        return stagingRepositoryId;
    }

    @Override
    protected String getDeployRepositoryURL()
        throws MojoExecutionException
    {
        stagingSiteURL = getStagingSiteURL( project, stagingSiteURL );

        getLog().info( "Using this URL for stage deploy: " + stagingSiteURL );

        return stagingSiteURL;
    }

    /**
     * Find the URL where staging will take place.
     *
     * @param currentProject      The currently executing project
     * @param reactorProjects     The projects in the reactor
     * @param usersStagingSiteURL The staging site URL as suggested by the user's configuration
     * @return the site URL for staging
     */
    private String getStagingSiteURL( MavenProject currentProject,
                                        String usersStagingSiteURL )
    {
        String topLevelURL = null;
        String relative = "";

        // If the user has specified a stagingSiteURL - use it
        if ( usersStagingSiteURL != null )
        {
            getLog().debug( "stagingSiteURL specified by the user: " + usersStagingSiteURL );
            topLevelURL = usersStagingSiteURL;
        }
        else
        {
            getLog().debug( "stagingSiteURL NOT specified by the user." );
        }

        MavenProject parentProject = getTopLevelParent( currentProject );

        // Take the distributionManagement site url from the top level project,
        // if there is one, otherwise take it from the current project
        if ( parentProject == null )
        {
            if ( topLevelURL == null )
            {
                // The user didn't specify a URL and there is no top level project in the reactor
                // Use current project
                topLevelURL =
                    currentProject.getDistributionManagement().getSite().getUrl() + "/" + DEFAULT_STAGING_DIRECTORY;
                getLog().debug( "No top level project found in the reactor, using the current project: " + topLevelURL );
            }
        }
        else
        {
            // Find the relative path between the parent and child distribution URLs, if any
            relative = "/" + siteTool.getRelativePath( currentProject.getDistributionManagement().getSite().getUrl(),
                                                       parentProject.getDistributionManagement().getSite().getUrl() );
            // SiteTool.getRelativePath() uses File.separatorChar, so we need to convert '\' to '/' in order for the URL
            // to be valid for Windows users
            relative = relative.replace( '\\', '/' );

            if ( topLevelURL == null )
            {
                // The user didn't specify a URL and there is a top level project in the reactor
                // Use the top level project
                topLevelURL =
                    parentProject.getDistributionManagement().getSite().getUrl() + "/" + DEFAULT_STAGING_DIRECTORY;
                getLog().debug( "Using the top level project: " + topLevelURL );
            }
        }

        // Return either
        //   usersURL + relative(from parent, to child)
        // or
        //   topLevelProjectURL + staging + relative(from parent, to child)
        return topLevelURL + relative;
    }
}
