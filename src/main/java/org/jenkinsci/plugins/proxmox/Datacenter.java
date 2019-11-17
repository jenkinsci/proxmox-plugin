package org.jenkinsci.plugins.proxmox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import org.jenkinsci.plugins.proxmox.pve2api.Connector;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import us.monoid.json.JSONException;

/**
 * Represents a Proxmox datacenter.
 */
public class Datacenter extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(Datacenter.class.getName());

    private final String hostname;
    private final String username;
    private final String realm;
    private final String password; //TODO: Use `Secret` to store the password.
    private final Boolean ignoreSSL;
    private transient Connector pveConnector;

    @DataBoundConstructor
    public Datacenter(String hostname, String username, String realm, String password, Boolean ignoreSSL) {
        super("Datacenter(proxmox)");
        this.hostname = hostname;
        this.username = username;
        this.realm = realm;
        this.password = password;
        this.ignoreSSL = ignoreSSL;
        this.pveConnector = null;
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

    public Boolean getIgnoreSSL() {
        return ignoreSSL;
    }

    public String getDatacenterDescription() {
        return username + "@" + realm + " - " + hostname;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public Connector proxmoxInstance() {
        if (pveConnector == null) {
            pveConnector = new Connector(hostname, username, realm, password, ignoreSSL);
        }
        return pveConnector;
    }

    public List<String> getNodes() {
        Connector pveConnector = proxmoxInstance();
        try {
            return pveConnector.getNodes();
        } catch (JSONException e) {
            return new ArrayList<String>(); //TODO: Properly log Proxmox exceptions
        } catch (LoginException e) {
            return new ArrayList<String>();
        } catch (IOException e) {
            return new ArrayList<String>();
        }
    }

    public HashMap<String, Integer> getQemuMachines(String node) {
        Connector pveConnector = proxmoxInstance();
        try {
            return pveConnector.getQemuMachines(node);
        } catch (JSONException e) {
            return new HashMap<String, Integer>();
        } catch (LoginException e) {
            return new HashMap<String, Integer>();
        } catch (IOException e) {
            return new HashMap<String, Integer>();
        }
    }

    public List<String> getQemuMachineSnapshots(String node, Integer vmid) {
        Connector pveConnector = proxmoxInstance();
        try {
            return pveConnector.getQemuMachineSnapshots(node, vmid);
        } catch (JSONException e) {
            return new ArrayList<String>();
        } catch (LoginException e) {
            return new ArrayList<String>();
        } catch (IOException e) {
            return new ArrayList<String>();
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {
        public String getDisplayName() {
            return "Proxmox Datacenter";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            save();
            return super.configure(req, o);
        }

        private FormValidation fieldNotSpecifiedError(String fieldName) {
            return FormValidation.error(fieldName + " not specified");
        }

        private FormValidation emptyStringValidation(String fieldName, String value) {
            if (Util.fixEmptyAndTrim(value) == null) return fieldNotSpecifiedError(fieldName);
            else return FormValidation.ok();
        }

        public FormValidation doCheckHostname(@QueryParameter String value) {
            return emptyStringValidation("Hostname", value);
        }

        public FormValidation doCheckUsername(@QueryParameter String value) {
            return emptyStringValidation("Username", value);
        }

        public FormValidation doCheckRealm(@QueryParameter String value) {
            return emptyStringValidation("Realm", value);
        }

        public FormValidation doCheckPassword(@QueryParameter String value) {
            return emptyStringValidation("Password", value);
        }

        public FormValidation doTestConnection (
                @QueryParameter String hostname, @QueryParameter String username, @QueryParameter String realm,
                @QueryParameter String password, @QueryParameter Boolean ignoreSSL) {
            try {
                if (hostname.isEmpty()) {
                    return fieldNotSpecifiedError("Hostname");
                }
                if (username.isEmpty()) {
                    return fieldNotSpecifiedError("Username");
                }
                if (realm.isEmpty()) {
                    return fieldNotSpecifiedError("Realm");
                }
                if (password.isEmpty()) {
                    return fieldNotSpecifiedError("Password");
                }

                Connector pveConnector = new Connector(hostname, username, realm, password, ignoreSSL);
                pveConnector.login();                                
                return FormValidation.ok("Login successful");

            } catch (LoginException e) {
                return FormValidation.error("Invalid login credentials");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error: " + e.getMessage());
                return FormValidation.error("Error: " + e.getMessage());
            }
        }

    }

}
