package pluginmanager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 * @author jayfella
 */

// Retrieves the list of plugins from jmonkey, initializes them, and finally clears the list.

public class PluginInitNewPlugins extends Task
{
    private String websiteUrl;
    public void setWebsite(String websiteUrl) { this.websiteUrl = websiteUrl; }

    private File suiteDirectory;
    public void setSuiteDirectory(File directory) { this.suiteDirectory = directory; }

    private String clearKey;
    public void setKey(String clearKey) { this.clearKey = clearKey; }

    @Override
    public void execute() throws BuildException
    {
        if (suiteDirectory == null)
            throw new BuildException("No directory specified. Aborted.");

        if (websiteUrl == null)
            throw new BuildException("No website specified. Aborted.");

        URL url;
        HttpURLConnection connection;
        List<String> gitUrls;

        try
        {
            url = new URL(websiteUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Jmonkey Build Server");

            int responseCode = connection.getResponseCode();

            if (responseCode > 302)
                throw new BuildException("Error: " + websiteUrl + " returned an error whilst reading list: " + responseCode);


            gitUrls = new ArrayList<>();

            try(InputStreamReader isReader = new InputStreamReader(connection.getInputStream()); BufferedReader bReader = new BufferedReader(isReader))
            {
                String inputLine;

                while ((inputLine = bReader.readLine()) != null)
                {
                    inputLine = inputLine.trim();

                    if (!inputLine.isEmpty())
                        gitUrls.add(inputLine);
                }
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            try(DataOutputStream dos = new DataOutputStream(connection.getOutputStream()))
            {
                dos.writeBytes("action=clearlist&key=" + clearKey);
                dos.flush();
            }

            responseCode = connection.getResponseCode();
            if (responseCode > 302)
                throw new BuildException("Error: " + websiteUrl + " returned an error while clearing list: " + responseCode);

        }
        catch (IOException ex)
        {
            throw new BuildException(ex);
        }

        for (String gitUrl : gitUrls)
        {
            // make a directory.
            int branchIndex = gitUrl.lastIndexOf(":");
            String repoUrl = gitUrl.substring(0, branchIndex);
            String branch = gitUrl.substring(branchIndex + 1);

            int urlIndex = repoUrl.lastIndexOf("/");
            String repoName = repoUrl.substring(urlIndex + 1);


            String newPluginPath = new StringBuilder()
                    .append(suiteDirectory.getAbsolutePath())
                    .append(File.separatorChar)
                    .append(repoName)
                    .toString();

            File newPluginDir = new File(newPluginPath);

            int directoryIterator = 1;

            while (newPluginDir.exists() && newPluginDir.isDirectory())
                newPluginDir = new File(newPluginPath + directoryIterator);

            if (!newPluginDir.mkdir())
            {
                System.out.println("Skipping " + repoName + " - Error creating directory.");
                continue;
            }

            // initialize the empty folder with git.
            try
            {
                ProcessBuilder builder = new ProcessBuilder("/usr/bin/git", "init", newPluginDir.getCanonicalPath())
                        .redirectErrorStream(true)
                        .directory(newPluginDir.getAbsoluteFile());

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
                continue;
            }

            // make a git.dat file.
            String gitDataPath = new StringBuilder()
                    .append(newPluginDir.getAbsolutePath())
                    .append(File.separatorChar)
                    .append(PluginUtils.GIT_DATA_FILENAME)
                    .toString();

            File gitDataFile = new File(gitDataPath);

            try
            {
                gitDataFile.createNewFile();
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.out);
                continue;
            }

            PluginUtils pluginUtils = new PluginUtils();

            Properties gitProperties = pluginUtils.loadProperties(gitDataFile);
            if (gitProperties == null)
            {
                System.out.println("Skipping " + repoName + " - Error initializing " + PluginUtils.GIT_DATA_FILENAME + " file.");
                continue;
            }

            gitProperties.setProperty(PluginUtils.PROP_URL, repoUrl);
            gitProperties.setProperty(PluginUtils.PROP_BRANCH, branch);

            try (FileOutputStream fos = new FileOutputStream(gitDataFile))
            {
                gitProperties.store(fos, "");
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.out);
                continue;
            }
        }
    }
}
