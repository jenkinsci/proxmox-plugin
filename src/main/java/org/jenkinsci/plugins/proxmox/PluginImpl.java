package org.jenkinsci.plugins.proxmox;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

import hudson.Plugin;
import hudson.slaves.Cloud;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public class PluginImpl extends Plugin {

    private static final java.util.logging.Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

    @Override
    public void start() throws Exception {
        LOGGER.log(Level.FINE, "Starting proxmox plugin");
    }

    @Override
    public void stop() throws Exception {
        LOGGER.log(Level.FINE, "Stopping proxmox plugin.");
    }

    public FormValidation doCheckStartupWaitingPeriodSeconds (@QueryParameter String secsValue)
            throws IOException, ServletException {
        try {
            int v = Integer.parseInt(secsValue);
            if (v < 0) {
                return FormValidation.error("Negative value..");
            } else {
                return FormValidation.ok();
            }
        } catch (NumberFormatException e) {
            return FormValidation.error("Not an integer");
        }
    }

    private Datacenter getDatacenterByDescription(String datacenterDescription) {
        for (Cloud cloud : Jenkins.getInstance().clouds) {
            if (cloud instanceof Datacenter) {
                Datacenter datacenter = (Datacenter) cloud;
                if (datacenterDescription != null
                        && datacenterDescription.equals(datacenter.getDatacenterDescription())) {
                    return datacenter;
                }
            }
        }
        return null;
    }

    public ListBoxModel doDatacenterNodeValues(@QueryParameter("desc") String datacenterDescription)
            throws IOException, ServletException {

        ListBoxModel m = new ListBoxModel();
        Datacenter datacenter = getDatacenterByDescription(datacenterDescription);
        if (datacenter != null) {
            List<String> nodes = datacenter.getNodes();
            for (String node : nodes) {
                m.add(new ListBoxModel.Option(node));
            }
            if (!m.isEmpty()) m.get(0).selected = true;
        }
        return m;
    }

    public ListBoxModel doVirtualMachineIdValues(@QueryParameter("desc") String datacenterDescription,
                                                 @QueryParameter("node") String datacenterNode)
            throws IOException, ServletException {

        ListBoxModel m = new ListBoxModel();
        Datacenter datacenter = getDatacenterByDescription(datacenterDescription);
        if (datacenter != null) {
            Map<String, Integer> names = datacenter.getQemuMachines(datacenterNode);
            for (Map.Entry<String, Integer> entry : names.entrySet()) {
                m.add(new ListBoxModel.Option(entry.getKey(), entry.getValue().toString()));
            }
            if (!m.isEmpty()) m.get(0).selected = true;
        }
        return m;
    }

    public ListBoxModel doSnapshotNameValues(@QueryParameter("desc") String datacenterDescription,
                                             @QueryParameter("node") String datacenterNode,
                                             @QueryParameter("vmid") String virtualMachineId)
            throws IOException, ServletException {

        ListBoxModel m = new ListBoxModel();
        Datacenter datacenter = getDatacenterByDescription(datacenterDescription);
        if (datacenter != null) {
            List<String> snapshots = datacenter.getQemuMachineSnapshots(datacenterNode,
                    Integer.parseInt(virtualMachineId));
            for (String snapshot : snapshots) {
                m.add(new ListBoxModel.Option(snapshot));
            }
            if (!m.isEmpty()) m.get(0).selected = true;
        }
        return m;
    }
}

