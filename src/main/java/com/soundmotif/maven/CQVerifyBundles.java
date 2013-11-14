package com.soundmotif.maven;

import com.soundmotif.cq.Bundle;
import com.soundmotif.cq.FelixConsoleResult;
import com.soundmotif.cq.FelixConsoleService;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author goirandt
 * @goal verify-bundles
 * @phase test
 */
public class CQVerifyBundles extends CQMojo {

    private MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager;
    private HttpClient client = null;

    protected URL getPackageManagerURL() throws MalformedURLException {
        return new URL(host + felixConsolePath + ".json");
    }

    protected URL getRegisteredServicesURL() throws MalformedURLException {
        return new URL(host + "/system/console/jmx/osgi.core%3Atype%3DbundleState%2Cversion%3D1.5/op/getRegisteredServices/long");
    }


    protected HttpClient getClient() {
        if (client == null) {
            multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, password);

            client = new HttpClient(multiThreadedHttpConnectionManager);
            client.getState().setCredentials(AuthScope.ANY, creds);

        }
        return client;
    }

    protected JSONObject retrievePackageManager() throws IOException, ParseException {
        getLog().info("Getting " + getPackageManagerURL().toString());

        GetMethod packmgr = new GetMethod(getPackageManagerURL().toString());
        getClient().executeMethod(packmgr);

        byte[] responseBody = packmgr.getResponseBody();
        JSONParser parser = new JSONParser();

        return (JSONObject) parser.parse(new String(responseBody));
    }

    protected void checkingPreConditions(FelixConsoleResult console) throws MojoFailureException {
        if (!console.areAllTheBundlesRunning()) {
            throw new MojoFailureException((console.getActiveBundle() + console.getFragmentsBundle()) + " bundles are  running out of "
                    + console.getTotalBundle());
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Bundle> bundlesInError = new ArrayList<Bundle>();

        try {
            FelixConsoleResult console = FelixConsoleService.populateFelixConsole(retrievePackageManager());
            checkingPreConditions(console);

            if (bundlesServiceExposed != null) {
                for (String s : bundlesServiceExposed) {
                    getLog().info("Checking bundle " + s);
                    Bundle b = FelixConsoleService.getBundle(console, s);
                    if (b == null)
                        throw new MojoFailureException("Bundle " + s + " not found");
                    if (howManyServiceExposed(b).size() == 0) {
                        getLog().warn(b.getName() + " is not exposing expected service at " + new URL(new URL(host + felixConsolePath + "/" + b.getId()).toString()));
                        bundlesInError.add(b);
                    }
                }
            } else {
                getLog().warn("No bundles defined as exposing services");
            }

            onCheck(bundlesInError);

        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoFailureException(e.getMessage());
        } catch (ParseException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoFailureException("can't parse JSON");
        }

    }

    protected void onCheck(List<Bundle> bundlesInError) throws MojoFailureException {
        if (bundlesInError.size() > 0) {
            throw new MojoFailureException(bundlesInError.size() + " are not exposing expected services");
        }
    }

    public Set<Long> howManyServiceExposed(Bundle b) throws IOException {
        PostMethod svcEx = new PostMethod(getRegisteredServicesURL().toString());

        getLog().debug("Checking services for " + b.getName() + " " + b.getId());

        svcEx.setQueryString(new NameValuePair[]{new NameValuePair("p1", b.getId().toString())});
        getClient().executeMethod(svcEx);

        if (svcEx.getStatusCode() != 200) {
            getLog().error("URL:" + getRegisteredServicesURL().toString() + " bad answer " + svcEx.getStatusCode());
            throw new NullPointerException();
        }

        String r = new String(svcEx.getResponseBody());

        Set<Long> services = new HashSet<Long>();
        String[] bd = r.split(",");
        for (String f : bd) {
            if (f.trim().length() > 0) {
                services.add(Long.parseLong(f.trim()));
            }
        }

        return services;
    }

}
