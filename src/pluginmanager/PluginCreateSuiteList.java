package pluginmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 * @author jayfella
 */

// Adds all new plugins to the plugin suite.

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
        Map<String, String> pluginData = new HashMap<>();
        
        File[] directories = suiteDirectory.listFiles(PluginUtils.DirectoryFilter);
        
        for (File directory : directories)
        {
            if (!pluginUtils.isProjectDir(directory))
                continue;
            
            if (!pluginUtils.isPluginDir(directory))
                continue;
            
            // get the base package from the manifest, and the Bundle.properties location too.
            
            String manifestFilePath = new StringBuilder()
                    .append(directory.getAbsolutePath())
                    .append(File.separatorChar)
                    .append("manifest.mf")
                    .toString();
            
            File manifestFile = new File(manifestFilePath);
            
            Properties manifestProperties = PluginUtils.loadProperties(manifestFile);
            
            String basePackage = manifestProperties.getProperty("OpenIDE-Module");
            
            String bundlePropertiesFilePath = new StringBuilder()
                    .append(directory.getAbsolutePath())
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
            
            pluginData.put(pluginName.trim(), basePackage.trim());
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
        Properties suiteProjProperties = PluginUtils.loadProperties(suiteProjPropertiesFile);
        
        Iterator<Map.Entry<String, String>> iterator = pluginData.entrySet().iterator();
        
        StringBuilder modulesValue = new StringBuilder();
        
        boolean isFirst = true;
        
        while (iterator.hasNext())
        {
            Map.Entry<String, String> entry = iterator.next();
            
            String projectVal = suiteProjProperties.getProperty("project." + entry.getValue());
            
            if (projectVal == null || projectVal.isEmpty())
            {
                suiteProjProperties.setProperty("project." + entry.getValue(), entry.getKey());
                System.out.println("Adding " + entry.getValue() + " to suite...");
            }

            if (!isFirst)
                modulesValue.append(":");
            
            modulesValue.append("${project.").append(entry.getValue()).append("}");
            isFirst = false;
        }
        
        suiteProjProperties.setProperty("modules", modulesValue.toString());
        
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
