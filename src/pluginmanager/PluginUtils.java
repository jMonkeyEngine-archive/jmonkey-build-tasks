package pluginmanager;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author jayfella
 */
public final class PluginUtils
{
    public static String GIT_DATA_FILENAME = "jme-git.dat";
    public static String PLUGIN_DATA_FILENAME = "jmeplugin.properties";

    public static String PROP_URL = "url";
    public static String PROP_BRANCH = "branch";

    public static FileFilter DirectoryFilter = new FileFilter()
    {
        @Override public boolean accept(File file) { return file.isDirectory(); }
    };

    public static final FileFilter FileFilter = new FileFilter()
    {
        @Override public boolean accept(File file) { return !file.isDirectory(); }
    };

    public boolean isProjectDir(File dir)
    {
        boolean manifestFile = false, buildFile = false;
        File[] files = dir.listFiles();
        if (files == null)
            return false;


        for (File file : files)
        {
            if (file.isDirectory())
                continue;

            if (file.getName().equalsIgnoreCase("manifest.mf")) manifestFile = true;
            if (file.getName().equalsIgnoreCase("build.xml")) buildFile = true;
            if (manifestFile && buildFile) return true;
        }

        System.out.println("Ignoring directory '" + dir.getName() + "' - Not a project.");
        return false;
    }

    public boolean isPluginDir(File dir)
    {
        // check for a ./nbproject/suite.properties file
        String suitePropertiesPath = new StringBuilder()
                .append(dir.getAbsolutePath())
                .append(File.separatorChar)
                .append("nbproject")
                .append(File.separatorChar)
                .append("suite.properties")
                .toString();

        File suitePropertiesFile = new File(suitePropertiesPath);
        if (!suitePropertiesFile.exists()) return false;

        // check the manifest file
        File manifestFile = new File(dir.getAbsolutePath() + File.separatorChar + "manifest.mf");
        if (!manifestFile.exists()) return false;

        // check if manifest.mf contains additional key:val data
        Properties manifestProperties = loadProperties(manifestFile);
        if (manifestProperties == null) return false;

        String basePackage = manifestProperties.getProperty("OpenIDE-Module");
        if (basePackage == null) return false;

        String bundlePropertiesPath = new StringBuilder()
                .append(dir.getAbsolutePath())
                .append(File.separatorChar)
                .append("src")
                .append(File.separatorChar)
                .append(basePackage.replace(".", "" + File.separatorChar))
                .append(File.separatorChar)
                .append("Bundle.properties")
                .toString();

        // check if basepackage contains a Bundle.properties file
        File bundlePropertiesFile = new File(bundlePropertiesPath);
        if (!bundlePropertiesFile.exists()) return false;

        System.out.println("Ignoring directory '" + dir.getName() + "' - Already a plugin.");
        return true;
    }

    public static Properties loadProperties(File propertiesFile)
    {
        try (FileInputStream fis = new FileInputStream(propertiesFile))
        {
            Properties properties = new Properties();
            properties.load(fis);
            return properties;
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.out);
            return null;
        }
    }

}
