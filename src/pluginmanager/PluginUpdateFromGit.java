package pluginmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import static pluginmanager.PluginUtils.DirectoryFilter;

/**
 *
 * @author jayfella
 */

// Sends a pull request on all plugins to retrieve the latest version.

public class PluginUpdateFromGit extends Task
{
    private File pluginDirectory;
    public void setDirectory(File directory) { this.pluginDirectory = directory; }

    @Override
    public void execute() throws BuildException
    {
        if (pluginDirectory == null)
            throw new BuildException("No directory specified. Aborted.");
        
        // has it been initialized by git?
        if (!isGitInitialized(pluginDirectory))
            return;

        String gitDataPath = new StringBuilder()
                    .append(pluginDirectory.getAbsolutePath())
                    .append(File.separatorChar)
                    .append(PluginUtils.GIT_DATA_FILENAME)
                    .toString();
            
            File gitDataFile = new File(gitDataPath);
        if (!gitDataFile.exists())
        {
            System.out.println("Ignoring directory '" + pluginDirectory.getName() + "' - Cannot locate '" + PluginUtils.GIT_DATA_FILENAME + "' file");
            return;
        }
        
        // everything appears ok - pull all changes.
        try
        {
            System.out.println("Pulling " + pluginDirectory.getName() + "...");

            Properties gitProperties = PluginUtils.loadProperties(gitDataFile);
            
            String gitUrl = gitProperties.getProperty(PluginUtils.PROP_URL);
            String gitBranch = gitProperties.getProperty(PluginUtils.PROP_BRANCH);

            ProcessBuilder builder = new ProcessBuilder("/usr/bin/git", "--git-dir=" + pluginDirectory.getCanonicalPath() + "/.git", "pull", gitUrl, gitBranch)
                    .redirectErrorStream(true)
                    .directory(pluginDirectory.getAbsoluteFile());

            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                String line;
                
                while ((line = reader.readLine()) != null)
                    System.out.println(line);
            }
            
            process.waitFor();
        }
        catch (IOException | InterruptedException ex)
        {
            ex.printStackTrace(System.out);
        }

    }
    
    private boolean isGitInitialized(File dir)
    {
        File[] dirs = dir.listFiles(DirectoryFilter);
        if (dirs == null) return false;

        for (File directory : dirs)
        {
            if (directory.getName().equalsIgnoreCase(".git"))
                return true;
        }

        System.out.println("Ingoring directory '" + pluginDirectory.getName() + "' - Does not contain .git folder.");
        return false;
    }

}
