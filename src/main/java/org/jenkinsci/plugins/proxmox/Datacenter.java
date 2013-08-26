package org.jenkinsci.plugins.proxmox;

import hudson.Util;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.slaves.Cloud;
import hudson.model.Label;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import net.sf.json.JSONObject;

import org.json.JSONException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import net.elbandi.pve2api.Pve2Api;

/**
 * Represents a Proxmox datacenter.
 */
public class Datacenter extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(Datacenter.class.getName());

    private final String hostname;
    private final String username;
    private final String realm;
    private final String password; //TODO: Use `Secret` to store the password.
    private transient Pve2Api pve_api;

    @DataBoundConstructor
    public Datacenter(String hostname, String username, String realm, String password) {
        super("Datacenter(proxmox)");
        this.hostname = hostname;
        this.username = username;
        this.realm = realm;
        this.password = password;
        this.pve_api = null;
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

    public String getDatacenterDescription() {
        return username + "@" + realm + " - " + hostname;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public Pve2Api proxmoxInstance() {
        if (pve_api == null) {
            pve_api = new Pve2Api(hostname, username, realm, password);
        }
        return pve_api;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        private String hostname;
        private String username;
        private String realm;
        private String password;

        public String getDisplayName() {
            return "Proxmox Datacenter";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            hostname = o.getString("hostname");
            username = o.getString("username");
            realm = o.getString("realm");
            password = o.getString("password");
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
                @QueryParameter String password) {
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

                Pve2Api pve_api = new Pve2Api(hostname, username, realm, password);
                pve_api.login();
                return FormValidation.ok("Login successful");

            } catch(JSONException e) {
                return FormValidation.error("Could not read server JSON response: " + e.getMessage());
            } catch (LoginException e) {
                return FormValidation.error("Invalid login credentials");
            } catch (IOException e) {
                return FormValidation.error("Error: " + e.getMessage());
            }
        }

    }

}
