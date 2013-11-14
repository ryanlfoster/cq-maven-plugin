package com.soundmotif.cq;

import java.util.ArrayList;
import java.util.List;

public class FelixConsoleResult {

    private String status;
    private Long totalBundle = new Long(0);
    private Long activeBundle;
    private Long fragmentsBundle;
    private Long resolvedBundle;
    private Long installedBundle;

    private List<Bundle> bundles;

    public FelixConsoleResult() {
        bundles = new ArrayList<Bundle>();
    }

    public boolean areAllTheBundlesRunning() {
        return totalBundle == activeBundle + fragmentsBundle;
    }

    public Long getTotalBundle() {
        return totalBundle;
    }

    public void setTotalBundle(Long totalBundle) {
        this.totalBundle = totalBundle;
    }

    public Long getActiveBundle() {
        return activeBundle;
    }

    public void setActiveBundle(Long activeBundle) {
        this.activeBundle = activeBundle;
    }

    public Long getFragmentsBundle() {
        return fragmentsBundle;
    }

    public void setFragmentsBundle(Long fragmentsBundle) {
        this.fragmentsBundle = fragmentsBundle;
    }

    public Long getResolvedBundle() {
        return resolvedBundle;
    }

    public void setResolvedBundle(Long resolvedBundle) {
        this.resolvedBundle = resolvedBundle;
    }

    public Long getInstalledBundle() {
        return installedBundle;
    }

    public void setInstalledBundle(Long installedBundle) {
        this.installedBundle = installedBundle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Bundle> getBundles() {
        return bundles;
    }

    public void setBundles(List<Bundle> bundles) {
        this.bundles = bundles;
    }

    public void addBundle(Bundle b) {
        this.bundles.add(b);
    }

}
