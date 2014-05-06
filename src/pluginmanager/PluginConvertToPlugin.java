package pluginmanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 * @author jayfella
 * Converts a regular project into a plugin
 */

public class PluginConvertToPlugin extends Task
{
    private File pluginDirectory;
    public void setDirectory(File directory) { this.pluginDirectory = directory; }

    @Override
    public void execute() throws BuildException
    {
        if (pluginDirectory == null)
            throw new BuildException("No directory specified. Aborted.");

        PluginUtils pluginUtils = new PluginUtils();

        // is it actually a project?
        if (!pluginUtils.isProjectDir(pluginDirectory))
        {
            System.out.println("Skipping directory '" + pluginDirectory.getName() + "' - Not a project.");
            return;
        }

        // is it already a plugin?
        if (pluginUtils.isPluginDir(pluginDirectory))
        {
            System.out.println("Skipping directory '" + pluginDirectory.getName() + "' - Already a plugin.");
            return;
        }

        File pluginDataFile = pluginUtils.recursiveSearch(pluginDirectory, PluginUtils.PLUGIN_DATA_FILENAME);

        if (pluginDataFile == null || !pluginDataFile.exists())
        {
            System.out.println("Ignoring directory '" + pluginDirectory.getName() + "' - Missing " + PluginUtils.PLUGIN_DATA_FILENAME + " file.");
            return;
        }

        String suitePropertiesFilePath = new StringBuilder()
                .append(pluginDirectory.getAbsolutePath())
                .append(File.separatorChar)
                .append("nbproject")
                .append(File.separatorChar)
                .append("suite.properties")
                .toString();

        File suitePropertiesFile = new File(suitePropertiesFilePath);
        File manifestFile = pluginUtils.getManifestFile(pluginDirectory);

        // check for a ./nbproject/suite.properties file
        if (!suitePropertiesFile.exists())
        {
            System.out.println("Creating suite.properties file...");

            try
            {
                suitePropertiesFile.createNewFile();

                try (FileWriter fw = new FileWriter(suitePropertiesFile.getAbsoluteFile()); BufferedWriter bw = new BufferedWriter(fw))
                {
                    bw.write("suite.dir=${basedir}/..");
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.out);
            }
        }

        // check if manifest.mf contains additional key:val data
        String basePackage = "";
        try
        {
            boolean modifiedManifestFile = false;

            Properties properties = new Properties();

            try(FileInputStream inputStream = new FileInputStream(manifestFile))
            {
                properties.load(inputStream);
            }

            if (properties.getProperty("Manifest-Version") == null)
            {
                properties.setProperty("Manifest-Version", "1.0");
                modifiedManifestFile = true;
            }

            if (properties.getProperty("OpenIDE-Module") == null)
            {
                properties.setProperty("OpenIDE-Module", "jmeplugin");
                modifiedManifestFile = true;
            }

            basePackage = properties.getProperty("OpenIDE-Module");

            if (properties.getProperty("OpenIDE-Module-Localizing-Bundle") == null)
            {
                properties.setProperty("OpenIDE-Module-Localizing-Bundle", basePackage.replace(".", "/") + "/Bundle.properties");
                modifiedManifestFile = true;
            }

            if (properties.getProperty("OpenIDE-Module-Specification-Version") == null)
            {
                properties.setProperty("OpenIDE-Module-Specification-Version", "1.0");
                modifiedManifestFile = true;
            }

            try(FileOutputStream outputStream = new FileOutputStream(manifestFile))
            {
                properties.store(outputStream, "");
            }

            if (modifiedManifestFile)
            {
                System.out.println("Adding plugin parameters to manifest.mf file...");
            }

        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.out);
        }

        // modify the project.xml file to reflect the new project type
        String projectXmlFilePath = new StringBuilder()
                .append(pluginDirectory.getAbsolutePath())
                .append(File.separatorChar)
                .append("nbproject")
                .append(File.separatorChar)
                .append("project.xml")
                .toString();

        File projectXmlFile = new File(projectXmlFilePath);
        if (!projectXmlFile.exists())
        {
            System.out.println("Error: Ignoring directory '" + pluginDirectory.getName() + "' - Unable to locate ./nbproject/project.xml file.");
            return;
        }
        else
        {
            projectXmlFile.delete();


            String xmlContent = new StringBuilder()
                    .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\n")
                    .append("<project xmlns=\"http://www.netbeans.org/ns/project/1\">").append("\n")
                    .append("\t<type>org.netbeans.modules.apisupport.project</type>").append("\n")
                    .append("\t<configuration>").append("\n")
                    .append("\t\t<data xmlns=\"http://www.netbeans.org/ns/nb-module-project/3\">").append("\n")
                    .append("\t\t\t<code-name-base>").append(basePackage).append("</code-name-base>").append("\n")
                    .append("\t\t\t<suite-component/>").append("\n")
                    .append("\t\t\t<module-dependencies/>").append("\n")
                    .append("\t\t\t<public-packages/>").append("\n")
                    .append("\t\t</data>").append("\n")
                    .append("\t</configuration>").append("\n")
                    .append("</project>").append("\n")
                    .toString();


            try (PrintWriter out = new PrintWriter(projectXmlFile))
            {
                projectXmlFile.createNewFile();
                out.write(xmlContent);
            }
            catch (FileNotFoundException ex)
            {
                ex.printStackTrace(System.out);
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.out);
            }
        }

        String bundlePropertiesFilePath = new StringBuilder()
                .append(pluginDirectory.getAbsolutePath())
                .append(File.separatorChar)
                .append("src")
                .append(File.separatorChar)
                .append(basePackage.replace(".", "" + File.separatorChar))
                .append(File.separatorChar)
                .append("Bundle.properties")
                .toString();

        File bundlePropertiesFile = new File(bundlePropertiesFilePath);
        if (!bundlePropertiesFile.exists())
        {
            try
            {
                System.out.println("Creating Bundle.properties file...");
                bundlePropertiesFile.getParentFile().mkdirs();
                bundlePropertiesFile.createNewFile();
            }
            catch(IOException ex)
            {
                ex.printStackTrace(System.out);
            }
        }

        // TODO: write the bundle properties file

        try
        {
            System.out.println("Updating Bundle.properties file with data from jmeplugin.properties file...");

            Properties properties = new Properties();

            try(FileInputStream inputStream = new FileInputStream(pluginDataFile))
            {
                properties.load(inputStream);
            }

            String displayName = properties.getProperty("DisplayName");
            String category = properties.getProperty("Category");
            String shortDesc = properties.getProperty("ShortDescription");
            String longDesc = properties.getProperty("LongDescription");

            // construct a new bundle.properties file
            String newBundlePropertiesContents = new StringBuilder()
                    .append("OpenIDE-Module-Name=").append(displayName).append(System.lineSeparator())
                    .append("OpenIDE-Module-Display-Category=").append(category).append(System.lineSeparator())
                    .append("OpenIDE-Module-Short-Description=").append(shortDesc).append(System.lineSeparator())
                    .append("OpenIDE-Module-Long-Description=").append(longDesc).append(System.lineSeparator())
                    .toString();

            // save the bundle.properties file
            try (FileWriter fw = new FileWriter(bundlePropertiesFile); BufferedWriter bw = new BufferedWriter(fw))
            {
                bw.write(newBundlePropertiesContents);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.out);
        }
    }
}
