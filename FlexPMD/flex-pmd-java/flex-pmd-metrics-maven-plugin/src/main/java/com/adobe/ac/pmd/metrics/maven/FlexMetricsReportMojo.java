/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.ac.pmd.metrics.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import com.adobe.ac.pmd.LoggerUtils;
import com.adobe.ac.pmd.metrics.maven.generators.NcssAggregateReportGenerator;
import com.adobe.ac.pmd.metrics.maven.generators.NcssReportGenerator;
import com.adobe.ac.pmd.metrics.maven.utils.ModuleReport;
import com.adobe.ac.pmd.metrics.maven.utils.NcssExecuter;

/**
 * @author xagnetti
 * @goal metrics
 */
public final class FlexMetricsReportMojo extends AbstractMavenReport
{
   private static final String OUTPUT_NAME = "javancss";

   private static ResourceBundle getBundle()
   {
      return ResourceBundle.getBundle( "flexMetrics" ); // NOPMD
   }

   /**
    * Specifies the maximum number of lines to take into account into the
    * reports.
    * 
    * @parameter default-value="5"
    * @required
    */
   private int                  lineThreshold;

   /**
    * Specifies the factor the mxml files nb of lines will be taken into account
    * 
    * @parameter default-value="0"
    * @required
    */
   private double               mxmlFactor;

   /**
    * Specifies the directory where the HTML report will be generated.
    * 
    * @parameter expression="${project.reporting.outputDirectory}"
    * @required
    * @readonly
    */
   private File                 outputDirectory;

   /**
    * @parameter expression="${project}"
    * @required
    * @readonly
    */
   private MavenProject         project;

   /**
    * The projects in the reactor for aggregation report.
    * 
    * @parameter expression="${reactorProjects}"
    * @readonly
    */
   private List< MavenProject > reactorProjects;
   /**
    * @parameter 
    *            expression="${component.org.apache.maven.doxia.siterenderer.Renderer}"
    * @required
    * @readonly
    */
   private Renderer siteRenderer;

   /**
    * Specifies the location of the source files to be used.
    * 
    * @parameter expression="${project.build.sourceDirectory}"
    * @required
    * @readonly
    */
   private File                 sourceDirectory;

   /**
    * Specified the name of the temporary file generated by Javancss prior
    * report generation.
    * 
    * @parameter default-value="javancss-raw-report.xml"
    * @required
    */
   private String               tempFileName;

   /**
    * Specifies the directory where the XML report will be generated.
    * 
    * @parameter default-value="${project.build.directory}"
    * @required
    */
   private File                 xmlOutputDirectory;

   public FlexMetricsReportMojo()
   {
      super();
   }

   public FlexMetricsReportMojo( final MavenProject projectToBeSet,
                                 final File source,
                                 final File output )
   {
      this();
      project = projectToBeSet;
      sourceDirectory = source;
      outputDirectory = output;
   }

   public void addReactorProject( final MavenProject project )
   {
      if ( reactorProjects == null )
      {
         reactorProjects = new ArrayList< MavenProject >();
      }
      reactorProjects.add( project );
   }

   @Override
   public void executeReport( final Locale locale ) throws MavenReportException
   {
      new LoggerUtils().loadConfiguration();
      if ( sourceDirectory != null )
      {
         if ( sourceDirectory.exists() )
         {
            try
            {
               generateSingleReport();
            }
            catch ( final DocumentException e )
            {
               throw new MavenReportException( e.getMessage(), e );
            }
            catch ( final IOException e )
            {
               throw new MavenReportException( e.getMessage(), e );
            }
         }
         else
         {
            getLog().error( "The source directory is not found "
                  + sourceDirectory.getAbsolutePath() );
         }
      }
      else
      {
         generateAggregateReport( locale );
      }
   }

   /**
    * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
    */
   public String getDescription( final Locale locale )
   {
      return getBundle().getString( "report.ncss.description" );
   }

   /**
    * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
    */
   public String getName( final Locale locale )
   {
      return getBundle().getString( "report.ncss.name" );
   }

   /**
    * @see org.apache.maven.reporting.MavenReport#getOutputName()
    */
   public String getOutputName()
   {
      return OUTPUT_NAME;
   }

   public void setLineThreshold( final int lineThresholdToBeSet )
   {
      lineThreshold = lineThresholdToBeSet;
   }

   public void setSiteRenderer( final Renderer siteRendererToBeSet )
   {
      siteRenderer = siteRendererToBeSet;
   }

   public void setTempFileName( final String tempFileNameToBeSet )
   {
      tempFileName = tempFileNameToBeSet;
   }

   public void setXmlOutputDirectory( final File xmlOutputDirectoryToBeSet )
   {
      xmlOutputDirectory = xmlOutputDirectoryToBeSet;
   }

   /**
    * Build a path for the output filename.
    * 
    * @return A String representation of the output filename.
    */
   /* package */File buildOutputFile()
   {
      return new File( getXmlOutputDirectory()
            + File.separator + tempFileName );
   }

   /**
    * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
    */
   @Override
   protected String getOutputDirectory()
   {
      return outputDirectory.getAbsolutePath();
   }

   /**
    * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
    */
   @Override
   protected MavenProject getProject()
   {
      return project;
   }

   /**
    * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
    */
   @Override
   protected Renderer getSiteRenderer()
   {
      return siteRenderer;
   }

   /**
    * Getter for the source directory
    * 
    * @return the source directory as a File object.
    */
   protected File getSourceDirectory()
   {
      return sourceDirectory;
   }

   protected String getXmlOutputDirectory()
   {
      return xmlOutputDirectory.getAbsolutePath();
   }

   private void generateAggregateReport( final Locale locale ) throws MavenReportException
   {
      final String basedir = project.getBasedir().toString();
      final String output = xmlOutputDirectory.toString();
      if ( getLog().isDebugEnabled() )
      {
         getLog().debug( "basedir: "
               + basedir );
         getLog().debug( "output: "
               + output );
      }
      String relative = null;
      if ( output.startsWith( basedir ) )
      {
         relative = output.substring( basedir.length() + 1 );
      }
      else
      {
         getLog().error( "Unable to aggregate report because I can't "
               + "determine the relative location of the XML report" );
         return;
      }
      getLog().debug( "relative: "
            + relative );
      final List< ModuleReport > reports = new ArrayList< ModuleReport >();

      if ( reactorProjects != null )
      {
         for ( final MavenProject mavenProject : reactorProjects )
         {
            final MavenProject child = mavenProject;
            final File xmlReport = new File( child.getBasedir() // NOPMD
                  + File.separator + relative, tempFileName );
            if ( xmlReport.exists() )
            {
               reports.add( new ModuleReport( child, loadDocument( xmlReport ) ) ); // NOPMD
            }
            else
            {
               getLog().debug( "xml file not found: "
                     + xmlReport );
            }
         }
         getLog().debug( "Aggregating "
               + reports.size() + " JavaNCSS reports" );

         new NcssAggregateReportGenerator( getSink(), getBundle(), getLog() ).doReport( locale,
                                                                                        reports,
                                                                                        lineThreshold );
      }
   }

   private void generateSingleReport() throws MavenReportException,
                                      DocumentException,
                                      IOException
   {
      if ( getLog().isDebugEnabled() )
      {
         getLog().debug( "Calling NCSSExecuter with src    : "
               + sourceDirectory );
         getLog().debug( "Calling NCSSExecuter with output : "
               + buildOutputFile() );
      }
      // run javaNCss and produce an temp xml file
      new NcssExecuter( sourceDirectory, buildOutputFile(), mxmlFactor ).execute();
      if ( !isTempReportGenerated() )
      {
         throw new MavenReportException( "Can't process temp ncss xml file." );
      }
      // parse the freshly generated file and write the report
      final NcssReportGenerator reportGenerator = new NcssReportGenerator( getSink(), getBundle(), getLog() );
      reportGenerator.doReport( loadDocument(),
                                lineThreshold );
   }

   /**
    * Check that the expected temporary file generated by JavaNCSS exists.
    * 
    * @return <code>true</code> if the temporary report exists,
    *         <code>false</code> otherwise.
    */
   private boolean isTempReportGenerated()
   {
      return buildOutputFile().exists();
   }

   private Document loadDocument() throws MavenReportException
   {
      return loadDocument( buildOutputFile() );
   }

   /**
    * Load the xml file generated by javancss. It first tries to load it as is.
    * If this fails it tries to load it with the forceEncoding parameter which
    * defaults to the system property "file.encoding". If this latter fails, it
    * throws a MavenReportException.
    */
   private Document loadDocument( final File file ) throws MavenReportException
   {
      try
      {
         return loadDocument( file,
                              null );
      }
      catch ( final DocumentException ignored )
      {
         try
         {
            return loadDocument( file,
                                 System.getProperty( "file.encoding" ) );
         }
         catch ( final DocumentException de )
         {
            throw new MavenReportException( de.getMessage(), de );
         }
      }
   }

   private Document loadDocument( final File file,
                                  final String encoding ) throws DocumentException
   {
      final SAXReader saxReader = new SAXReader();
      if ( encoding != null )
      {
         saxReader.setEncoding( encoding );
         getLog().debug( "Loading xml file with encoding : "
               + encoding );
      }
      return saxReader.read( file );
   }
}
