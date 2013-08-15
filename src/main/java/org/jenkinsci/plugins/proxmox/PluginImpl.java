package org.jenkinsci.plugins.proxmox;

import hudson.Plugin;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

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
                return FormValidation.warning("You declared this virtual machine to be ready right away. It probably needs a couple of seconds before it is ready to process jobs!");
            } else {
                return FormValidation.ok();
            }
        } catch (NumberFormatException e) {
            return FormValidation.error("Not a number..");
        }
    }

}

