package org.jenkinsci.plugins.proxmox;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.slaves.Cloud;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class PluginImpl extends Plugin {

    private static final java.util.logging.Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

    @Override
    public void start() throws Exception {
        LOGGER.log(Level.FINE, "Starting proxmox plugin");
    }

    /**
     * {@inheritDoc}
     */
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
            } else if (v == 0) {
                return FormValidation.warning("You declared this virtual machine to be ready right away. " +
                        "It probably needs a couple of seconds before it is ready to process jobs!");
            } else {
                return FormValidation.ok();
            }
        } catch (NumberFormatException e) {
            return FormValidation.error("Not a number..");
        }
    }

    //TODO: Might not need the verbose "@QueryParameter" for the function arguments below...

    //TODO: Get names of nodes on Proxmox.
    public void doDatacenterNodeValues(StaplerRequest req, StaplerResponse rsp,
                                       @QueryParameter("datacenterDescription") String datacenterDescription)
            throws IOException, ServletException {
        ListBoxModel m = new ListBoxModel();
        String nodeName = "Some Test Node";
        m.add(new ListBoxModel.Option(nodeName, nodeName));
        m.get(0).selected = true;
        m.writeTo(req, rsp);
    }

    //TODO: Get names of virtual machines
    public void doVirtualMachineNameValues(StaplerRequest req, StaplerResponse rsp,
                                           @QueryParameter("datacenterDescription") String datacenterDescription,
                                           @QueryParameter("datacenterNode") String datacenterNode)
            throws IOException, ServletException {
        ListBoxModel m = new ListBoxModel();
        String vmName = "Some Test Machine";
        m.add(new ListBoxModel.Option(vmName, vmName));
        m.get(0).selected = true;
        m.writeTo(req, rsp);
    }

    //TODO: Get the names of the virtual machines snapshots
    public void doSnapshotNameValues(StaplerRequest req, StaplerResponse rsp,
                                     @QueryParameter("datacenterDescription") String datacenterDescription,
                                     @QueryParameter("datacenterNode") String datacenterNode,
                                     @QueryParameter("virtualMachineName") String virtualMachineName)
            throws IOException, ServletException {
        ListBoxModel m = new ListBoxModel();
        String snapshotName = "Some Test Snapshot";
        m.add(new ListBoxModel.Option(snapshotName, snapshotName));
        m.get(0).selected = true;
        m.writeTo(req, rsp);
    }
}

