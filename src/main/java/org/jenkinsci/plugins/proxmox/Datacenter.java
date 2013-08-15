package org.jenkinsci.plugins.proxmox;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.slaves.Cloud;
import hudson.model.Label;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Represents a Proxmox datacenter.
 */
public class Datacenter extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(Datacenter.class.getName());

    private final String hostname;
    private final String username;
    private final String realm;
    private final String password;
    private final String nodeIndex;
    private transient String authTicket;
    private transient String CSRFPreventionToken;

    @DataBoundConstructor
    public Datacenter(String hostname, String username, String realm, String password, String nodeIndex) {
        super("Datacenter(proxmox)");
        this.hostname = hostname;
        this.username = username;
        this.realm = realm;
        this.password = password;
        this.nodeIndex = nodeIndex;
    }

    public Collection<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {
        return Collections.emptySet();
    }

    public boolean canProvision(Label label) {
        return false;
    }

    public String getHostname() {
        return hostname;
    }

    public String getUsername() {
        return username;
    }

    public String getRealm() {
        return realm;
    }

    public String getPassword() {
        return password;
    }

    public String getNodeIndex() {
        return nodeIndex;
    }

    public String getDatacenterDescription() {
        return getUsername() + "@" + getRealm() + " - " + getHostname() + "/" + getNodeIndex();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        private String hostname;
        private String username;
        private String realm;
        private String password;
        private String nodeIndex;

        public String getDisplayName() {
            return "Proxmox datacenter";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            hostname = o.getString("hostname");
            username = o.getString("username");
            realm = o.getString("realm");
            password = o.getString("password");
            nodeIndex = o.getString("nodeIndex");
            save();
            return super.configure(req, o);
        }

        public FormValidation doTestConnection(
                @QueryParameter String hostname, @QueryParameter String username, @QueryParameter String realm,
                @QueryParameter String password, @QueryParameter String nodeIndex) throws Exception, ServletException {
            try {
                if (hostname == null) {
                    return FormValidation.error("Proxmox Hostname is not specified!");
                }
                if (username == null) {
                    return FormValidation.error("Proxmox username is not specified!");
                }
                if (realm == null) {
                    return FormValidation.error("Proxmox user realm is not specified!");
                }
                if (password == null) {
                    return FormValidation.error("Proxmox password is not specified!");
                }
                if (nodeIndex == null) {
                    return FormValidation.error("Proxmox node index is not specified!");
                }

                //TODO: Test the connection to the Proxmox cluster.

                return FormValidation.ok("OK: " + hostname);

            } catch (UnsatisfiedLinkError e) {
                LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to hypervisor. Check libvirt installation on jenkins machine!");
                rec.setThrown(e);
                rec.setParameters(new Object[]{hostname, username});
                LOGGER.log(rec);
                return FormValidation.error(e.getMessage());
            } catch (Exception e) {
                LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to hypervisor. Check libvirt installation on jenkins machine!");
                rec.setThrown(e);
                rec.setParameters(new Object[]{hostname, username});
                LOGGER.log(rec);
                return FormValidation.error(e.getMessage());
            }
        }

        public String getHostname() {
            return hostname;
        }

        public String getUsername() {
            return username;
        }

        public String getRealm() {
            return realm;
        }

        public String getPassword() {
            return password;
        }

        public String getNodeIndex() {
            return nodeIndex;
        }

    }

}
