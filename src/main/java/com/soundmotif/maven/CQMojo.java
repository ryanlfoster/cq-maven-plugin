package com.soundmotif.maven;

import org.apache.commons.httpclient.HttpURL;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public abstract class CQMojo extends AbstractMojo {

    /**
     * @parameter expression:"${host}" default-value="localhost"
     */
    protected String host;

    /**
     * @parameter expression:"${port}" default-value="4502"
     */
    protected String port;

    /**
     * @parameter expression:"${user}" default-value="admin"
     */
    protected String user;

    /**
     * @parameter expression:"${password}" default-value="admin"
     */
    protected String password;


    /**
     * @parameter expression:"${path}" default-value="/apps/vaa/install/"
     */
    protected String path;

    /**
     * @parameter expression:"${path}"
     * default-value="http://localhost:8080/crx/server"
     */
    protected String webDavServerPath;

    /**
     * The built jar.
     *
     * @parameter default-value=
     * "${project.build.directory}/${project.artifactId}-${project.version}.jar"
     * @required
     */
    protected File jarFile;

    /**
     * @parameter expression:"${packMgrPath}"
     * default-value="/crx/packmgr/service.jsp"
     */
    protected String packMgrPath;

    /**
     * @parameter
     */
    protected List<String> bundlesServiceExposed;

    /**
     * @parameter default-value="/system/console/bundles"
     * @required
     */
    protected String felixConsolePath;

    /**
     * Full URL to access to the server.
     */
    protected HttpURL url;

    /**
     * @parameter default-value= "${project.build.directory}/classes/jcr_root"
     * @required
     */
    protected File syncSrcFile;

    /**
     * @parameter default-value= "${project.build.directory}/syncRefFile.xml"
     * @required
     */
    protected File syncRefFile;


    public final static String JCR_ROOT = "jcr_root";

    public abstract void execute() throws MojoExecutionException,
            MojoFailureException;

    @Override
    public void setPluginContext(Map pluginContext) {
        super.setPluginContext(pluginContext);
        try {
            init();
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
        }
    }

    /**
     * Set the default parameters
     *
     * @throws IOException
     */
    protected void init() throws IOException {
        // set default values.
        if (user == null)
            user = "admin";

        if (password == null)
            password = "admin";

        if (host == null)
            host = "localhost";

        if (port == null)
            port = "4502";

        if (path == null)
            path = "apps/vaa/install/";

        if (packMgrPath == null)
            packMgrPath = "/crx/packmgr/service/";

        if (webDavServerPath == null)
            webDavServerPath = "http://localhost:4502/crx/server";

    }

    private String openManifestAndCheck(InputStream is) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String s;
            while ((s = br.readLine()) != null) {
                if (s.startsWith("Bundle-SymbolicName: ")) {
                    is.close();
                    return s.substring("Bundle-SymbolicName: ".length());
                }
            }
            is.close();
        } catch (IOException e) {
            getLog().error("Can't open Manifest", e);
            return null;
        }
        return null;
    }

    /**
     * Retrieve the name of a OSGi bundle.
     *
     * @return name of the current bunlde, null if it is not a bundle.
     * @throws IOException
     */
    public String getBundleName() throws IOException {
        ZipFile zf = new ZipFile(jarFile);
        ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");

        if (ze == null) {
            zf.close();
            return null;
        }

        return openManifestAndCheck(zf.getInputStream(ze));
    }

    /**
     * @return
     * @throws IOException
     */
    public boolean isPackage() throws IOException {
        ZipInputStream zip = new ZipInputStream(new FileInputStream(jarFile));

        ZipEntry ze;

        while ((ze = zip.getNextEntry()) != null) {
            if (ze.getName().startsWith(JCR_ROOT)) {
                zip.close();
                return true;
            }

        }

        zip.close();
        return false;
    }

}
