package com.soundmotif.maven;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import javax.jcr.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides essentials parameters and methods to access to CQ5 using WebDav
 *
 * @author goirandt
 */
public abstract class CQWebDavMojo extends CQMojo {

    Session session = null;

    public void connect() throws RepositoryException {
        Repository repository = JcrUtils.getRepository(webDavServerPath);
        session = repository.login(new javax.jcr.SimpleCredentials(user,
                password.toCharArray()));
    }

    protected Node get(String parent, String name) throws PathNotFoundException,
            RepositoryException {

        Node n = session.getNode(parent);
        getLog().debug("parents" + n.getParent().getName());
        for (NodeIterator it = n.getNodes(); it.hasNext(); ) {
            Node child = (Node) it.next();
            getLog().debug("children:" + child.getName());
            return child;
        }
        return null;
    }

    public void update(String parent, String name, File source) throws MojoExecutionException {
        try {
            if (session == null)
                connect();

            Node node = get(parent, name);
            node = node.getNodes("jcr:content").nextNode();

            ValueFactory valueFactory = session.getValueFactory();

            Binary binary = session.getValueFactory().createBinary(
                    new FileInputStream(source));

            Property data = node.getProperty("jcr:data");
            data.setValue(binary);

            Property p = node.getProperty("jcr:lastModified");
            p.setValue(valueFactory.createValue(Calendar.getInstance()));

            session.save();

        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Path not found: "
                    + source.getAbsolutePath(), e);
        } catch (RepositoryException e) {
            throw new MojoExecutionException(
                    "Can't connect to the repository: " + e.getMessage(), e);
        }
    }

    public void put(File source, String destFile) throws MojoExecutionException {

        if (getLog().isDebugEnabled()) {
            getLog().debug("source:" + source.getAbsolutePath());
            getLog().debug("dest:" + destFile);
        }

        try {
            if (session == null)
                connect();

            Node n = session.getNode(destFile);

            getLog().debug("Node:" + n.getPath());

            n = n.getNodes("jcr:content").nextNode();

            ValueFactory valueFactory = session.getValueFactory();

            Binary binary = session.getValueFactory().createBinary(
                    new FileInputStream(source));

            Property data = n.getProperty("jcr:data");
            data.setValue(binary);

            Property p = n.getProperty("jcr:lastModified");
            p.setValue(valueFactory.createValue(Calendar.getInstance()));

            session.save();

        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Path not found: "
                    + source.getAbsolutePath(), e);
        } catch (PathNotFoundException e) {

            throw new MojoExecutionException("Path not found: " + destFile, e);
        } catch (RepositoryException e) {
            throw new MojoExecutionException(
                    "Can't connect to the repository: " + e.getMessage(), e);
        }

    }

    /**
     * Delete a file on a webdav URL
     *
     * @param path
     * @throws IOException
     * @throws IOException
     * @throws MojoExecutionException
     */
    protected void delete(String path) throws IOException,
            MojoExecutionException {

        getLog().info("Deleting " + url.getURI());

    }

    /**
     * Known Mimetypes to override default.
     *
     * @return
     */
    protected Map<String, String> getMimetypeMap() {
        Map<String, String> res = new HashMap<String, String>();
        res.put("xml", "text/xml");
        res.put("js", "application/x-javascript");
        res.put("jsp", "application/x-html");
        return res;
    }

    /**
     * Return the path on the server.
     *
     * @return The path on the server.
     */
    protected String getPath() {

        path = StringUtils.replace(path, "\\", "/");

        if (!path.endsWith("/"))
            path += "/";

        if (!path.startsWith("/"))
            path = "/" + path;

        return path + jarFile.getName();
    }

}
