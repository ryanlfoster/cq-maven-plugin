package com.soundmotif.maven;

import com.soundmotif.cq.Bundle;
import com.soundmotif.cq.FelixConsoleResult;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * @author goirandt
 * @goal restart-bundles
 * @phase test
 */
public class CQRestartBundles extends CQVerifyBundles {


    @Override
    protected void checkingPreConditions(FelixConsoleResult console) throws MojoFailureException {
        // nothing to do.
    }

    protected void stopBundle(Bundle bundle) throws IOException {
        getLog().info("Stopping " + bundle.getName());
        URL stopUrl = new URL(host + felixConsolePath + "/" + bundle.getId().toString());
        getLog().debug("stop url:" + stopUrl.toString());
        PostMethod stopMethod = new PostMethod(stopUrl.toString());
        stopMethod.setParameter("action", "stop");
        getClient().executeMethod(stopMethod);
        stopMethod.getResponseBody();
    }

    protected void startBundle(Bundle bundle) throws IOException {
        getLog().info("Starting " + bundle.getName());
        URL stopUrl = new URL(host + felixConsolePath + "/" + bundle.getId().toString());
        getLog().debug("stop url:" + stopUrl.toString());
        PostMethod stopMethod = new PostMethod(stopUrl.toString());
        stopMethod.setParameter("action", "start");
        getClient().executeMethod(stopMethod);
        stopMethod.getResponseBody();
    }


    @Override
    protected void onCheck(List<Bundle> bundlesInError) throws MojoFailureException {

        for (Bundle bundle : bundlesInError) {
            try {
                stopBundle(bundle);
            } catch (IOException e) {
                getLog().error("Can't stop bundle " + bundle.getName(), e);
            }

            try {
                startBundle(bundle);
            } catch (IOException e) {
                getLog().error("Can't start bundle " + bundle.getName(), e);
            }


        }


    }
}
