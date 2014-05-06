package pluginmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 * @author jayfella
 * Adds all new plugins to the plugin suite.
 */

public class PluginCreateSuiteList extends Task
{
    private File suiteDirectory;
    public void setSuiteDirectory(File directory) { this.suiteDirectory = directory; }

    @Override
    public void execute() throws BuildException
    {
        if (suiteDirectory == null)
            throw new BuildException("No directory specified. Aborted.");

        PluginUtils pluginUtils = new PluginUtils();
        List<PluginData> pluginData = new ArrayList<>();

        File[] directories = suiteDirectory.listFiles(PluginUtils.DirectoryFilter);

        for (File pluginDirectory : directories)
        {
            if (!pluginUtils.isProjectDir(pluginDirectory))
            {
                System.out.println("Skipping directory '" + pluginDirectory.getName() + "' - Not a project.");
                continue;
            }

            if (!pluginUtils.isPluginDir(pluginDirectory))
            {
                System.out.println("Skipping directory '" + pluginDirectory.getName() + "' - Not a plugin.");
                continue;
            }

            // get the base package from the manifest, and the Bundle.properties location too.
            File manifestFile = pluginUtils.getManifestFile(pluginDirectory);

            Properties manifestProperties = pluginUtils.loadProperties(manifestFile);

            String basePackage = manifestProperties.getProperty("OpenIDE-Module");

            String bundlePropertiesFilePath = new StringBuilder()
                    .append(pluginDirectory.getAbsolutePath())
                    .append(File.separatorChar)
                    .append("src")
                    .append(File.separatorChar)
                    .append(manifestProperties.getProperty("OpenIDE-Module-Localizing-Bundle").replace("/", "" + File.separatorChar))
                    .toString();

            File bundlePropertiesFile = new File(bundlePropertiesFilePath);

            String[] lines;
            String pluginName = "";

            try
            {
                lines = new String(Files.readAllBytes(bundlePropertiesFile.toPath())).split(System.lineSeparator());
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.out);
                continue;
            }

            for (String line : lines)
            {
                if (line.startsWith("OpenIDE-Module-Name"))
                {
                    int valueSplit = line.indexOf("=");
                    pluginName = line.substring(valueSplit + 1);
                }
            }

            pluginData.add(new PluginData(pluginName.trim(), basePackage.trim(), pluginDirectory.getName()));
        }

        // add data to project.properties suite file
        String suiteProjPropertiesPath = new StringBuilder()
                .append(suiteDirectory)
                .append(File.separatorChar)
                .append("nbproject")
                .append(File.separatorChar)
                .append("project.properties")
                .toString();

        File suiteProjPropertiesFile = new File(suiteProjPropertiesPath);
        Properties suiteProjProperties = pluginUtils.loadProperties(suiteProjPropertiesFile);

        StringBuilder modulesValue = new StringBuilder();

        boolean isFirst = true;
        int newPluginCount = 0;

        for (PluginData data : pluginData)
        {
            String projectVal = suiteProjProperties.getProperty("project." + data.basePackage);

            if (projectVal == null || projectVal.isEmpty())
            {
                suiteProjProperties.setProperty("project." + data.basePackage, data.dirName);
                System.out.println("Adding '" + data.pluginName + "' to suite...");

                newPluginCount++;
            }
            else
            {
                System.out.println("Ignoring '" + data.pluginName + "' - already added.");
            }

            if (!isFirst)
                modulesValue.append(":");

            modulesValue.append("${project.").append(data.basePackage).append("}");
            isFirst = false;
        }

        suiteProjProperties.setProperty("modules", modulesValue.toString());
        System.out.println("Added " + newPluginCount + " new plugins.");

        try (FileOutputStream fos = new FileOutputStream(suiteProjPropertiesFile))
        {
            suiteProjProperties.store(fos, "");
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.out);
        }
    }

}

class PluginData
{
    public final String pluginName;
    public final String basePackage;
    public final String dirName;

    public PluginData(String pluginName, String basePackage, String dirName)
    {
        this.pluginName = pluginName;
        this.basePackage = basePackage;
        this.dirName = dirName;
    }
}