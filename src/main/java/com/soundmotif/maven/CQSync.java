package com.soundmotif.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Send the most recent files to CQ5
 *
 * @author goirandt
 * @goal sync
 * @phase process-resources
 */
public class CQSync extends CQWebDavMojo {

    /**
     * The output directory into which to copy the resources.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The list of resources we want to transfer.
     *
     * @parameter default-value="${project.resources}"
     * @required
     * @readonly
     */
    private List<Resource> resources;

    /**
     * Time delayed in milliseconds if no ref file is found
     *
     * @parameter default-value="100000"
     * @required
     */
    private Long delayMax;

    /**
     * The output directory into which to copy the resources.
     *
     * @parameter default-value="${project.build.outputDirectory}/timeref"
     * @required
     */
    private File timeRefFile;

    // TODO to make it configurable.
    static Set<String> exclude = new HashSet<String>();
    static Set<String> excludingEndWidth = new HashSet<String>();

    static {
        // excluding vlt files internal mac files
        exclude.add(".vlt");
        exclude.add(".DS_Store");

        // excluding files to avoid conflict with SVN or VLT
        excludingEndWidth.add(".mine");
        excludingEndWidth.add(".theirs");
        excludingEndWidth.add(".base");
        excludingEndWidth.add(".dir");
    }

    public void execute() throws MojoExecutionException {

        if (delayMax == null)
            delayMax = new Long(100000);

        Date lastTransfer = loadTimeRef();
        if (lastTransfer == null)
            lastTransfer = new Date(System.currentTimeMillis()
                    - delayMax.intValue());

        List<File> resourceFile = new ArrayList<File>();
        // adding resources folders
        for (Resource r : resources)
            resourceFile.add(new File(new File(r.getDirectory()),
                    CQMojo.JCR_ROOT));
        // listing subfolders
        Set<File> filesToSend = sublistFiles(resourceFile, lastTransfer);

        if (getLog().isInfoEnabled())
            getLog().info("Files to be sent:" + filesToSend.toString());

        for (File f : filesToSend) {
            if (getLog().isDebugEnabled())
                getLog().debug(f.getAbsolutePath());

            File dest = getOutputFile(f);

            if (getLog().isDebugEnabled())
                getLog().debug(dest.getAbsolutePath());

            if (isValid(dest))
                put(dest, getTargetPath(dest));
        }

        if (filesToSend.isEmpty())
            saveTimeRef(new Date());
        else
            saveTimeRef(new Date(filesToSend.iterator().next().lastModified()));
    }

    /**
     * Check whether the file is eligible to be sent.
     *
     * @param output
     * @return
     */
    protected boolean isValid(File output) {
        if (!output.isFile())
            return false;

        File root = new File(outputDirectory, CQMojo.JCR_ROOT);
        getLog().debug(output.getAbsolutePath() + ":" + root.getAbsolutePath());
        return output.getAbsolutePath().indexOf(root.getAbsolutePath()) == 0;
    }

    /**
     * Get the path on the server
     *
     * @param output
     * @return
     */
    protected String getTargetPath(File output) {
        File root = new File(outputDirectory, CQMojo.JCR_ROOT);
        getLog().debug("Output path:" + output.getAbsolutePath());
        return convertPath(output.getAbsolutePath().substring(
                root.getAbsolutePath().length()));
    }

    /**
     * Convert path string from win file system.
     *
     * @param path
     * @return
     */
    private String convertPath(String path) {
        return StringUtils.replace(path, "\\", "/");
    }

    /**
     * Get the file in the output folder
     *
     * @param f
     * @return
     */
    protected File getOutputFile(File f) {
        for (Resource res : resources) {
            File r = new File(res.getDirectory());
            getLog().debug(f.getAbsolutePath() + ":" + r.getAbsolutePath());
            if (f.getAbsolutePath().startsWith(r.getAbsolutePath()))
                return new File(outputDirectory, f.getAbsolutePath().substring(
                        r.getAbsolutePath().length()));
        }
        // shouldn't happen
        throw new RuntimeException("Resources directories don't match");
    }

    static private boolean isResourceValid(String name) {
        if (exclude.contains(name)) {
            return false;
        }

        for (String ew : excludingEndWidth) {
            if (name.endsWith(ew)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Recursively list the subfolders
     *
     * @param files
     * @param modifiedDate
     * @return Files modified after the date provided.
     */
    protected Set<File> sublistFiles(List<File> files, final Date modifiedDate) {

        Set<File> result = new TreeSet<File>(new Comparator<File>() {
            public int compare(File o1, File o2) {
                // let's got the newer files on the top
                StringBuilder sb2 = new StringBuilder(new Long(
                        o2.lastModified()).toString());
                sb2.append(o2.getAbsolutePath());

                StringBuilder sb1 = new StringBuilder(new Long(
                        o1.lastModified()).toString());
                sb1.append(o1.getAbsolutePath());

                return sb2.toString().compareTo(sb1.toString());
            }

        });

        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            if (f.isDirectory())
                for (File subFiles : f.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {

                        boolean res = isResourceValid(pathname.getName());
                        if (res == false) {
                            if (getLog().isDebugEnabled())
                                getLog().debug(
                                        "Excluding " + pathname.getName());
                            return false;
                        }

                        if (pathname.isDirectory()) {
                            return true;
                        }

                        if (pathname.lastModified() <= modifiedDate.getTime()) {
                            return false;
                        }

                        if (getLog().isDebugEnabled())
                            getLog().debug(
                                    pathname.getAbsolutePath() + ":"
                                            + pathname.lastModified() + ":"
                                            + modifiedDate.getTime());
                        return true;
                    }
                }))
                    if (subFiles.isFile())
                        result.add(subFiles);
                    else
                        files.add(subFiles);
        }
        return result;
    }

    protected Date loadTimeRef() {
        if (!timeRefFile.exists())
            return null;
        if (getLog().isDebugEnabled()) {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy.MM.dd G 'at' HH:mm:ss z");
            getLog().debug(
                    "Last transfer was "
                            + sdf.format(new Date(timeRefFile.lastModified())));
        }

        return new Date(timeRefFile.lastModified());
    }

    protected void saveTimeRef(Date lastModified) {

        try {
            if (timeRefFile.exists())
                timeRefFile.delete();
            timeRefFile.createNewFile();
            timeRefFile.setLastModified(lastModified.getTime());
        } catch (IOException e) {
            getLog().error("Can't save ref file", e);
        }

    }
}
