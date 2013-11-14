package com.soundmotif.maven;

import com.soundmotif.cq.Bundle;
import com.soundmotif.cq.FelixConsoleResult;
import com.soundmotif.cq.FelixConsoleService;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;

public class CQ5VerifyBundlesTest extends AbstractMojoTestCase {
    CQVerifyBundles mojo;
    FelixConsoleResult consoleResult;

    protected void setUp() throws Exception {

        // required for mojo lookups to work
        super.setUp();

        File testPom = new File(getBasedir(), "src/test/resources/test-plugin-config.xml");

        mojo = (CQVerifyBundles) lookupMojo("verify-bundles", testPom);
        assertNotNull(mojo);
        assertNotNull(mojo.felixConsolePath);
        consoleResult = FelixConsoleService.populateFelixConsole(mojo.retrievePackageManager());
        assertNotNull(consoleResult);
    }

    public void testMojoExecute() throws Exception {

        try {
            mojo.execute();
        } catch (MojoFailureException e) {
            assertEquals("1 are not exposing expected services", e.getMessage());
        }


    }

    public void testServicesExposed() throws Exception {

        assertTrue(mojo.bundlesServiceExposed.size() > 0);

        for (String s : mojo.bundlesServiceExposed) {
            assertNotNull(s);
            Bundle b = FelixConsoleService.getBundle(consoleResult, s);
            if (!b.getSymbolicName().equals("com.adobe.sharedcloud.worker.XMPFilesProcessor.native.fragment.linux"))
                assertTrue(mojo.howManyServiceExposed(b).size() > 0);
            else
                assertTrue(mojo.howManyServiceExposed(b).size() == 0);
        }


    }

}
