package org.jenkinsci.plugins.proxmox.pve2api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;
import static us.monoid.web.Resty.*;

import javax.security.auth.login.LoginException;

public class Connector {
    protected String hostname;
    protected Integer port;
    protected String username;
    protected String realm;
    protected String password;
    protected String baseURL;

    private String authTicket;
    private Date authTicketIssuedTimestamp;
    private String csrfPreventionToken;

    public Connector(String hostname, String username, String realm, String password) {
        //TODO: Split the hostname to check for a port.
        this.hostname = hostname;
        this.port = 8006;
        this.username = username;
        this.realm = realm;
        this.password = password;
        this.authTicketIssuedTimestamp = null;
        this.baseURL = "https://" + hostname + ":" + port.toString() + "/api2/json/";
    }

    public void login() throws IOException, LoginException {
        Resty r = new Resty();
        JSONResource authTickets = r.json(baseURL + "access/ticket",
                form(data("username", username + "@" + realm), data("password", password)));
        try {
            authTicket = authTickets.get("data.ticket").toString();
            csrfPreventionToken = authTickets.get("data.CSRFPreventionToken").toString();
            authTicketIssuedTimestamp = new Date();
        } catch (Exception e) {
            throw new LoginException("Failed reading JSON response");
        }
    }

    public void checkIfAuthTicketIsValid() throws IOException, LoginException {
        if (authTicketIssuedTimestamp == null
                || authTicketIssuedTimestamp.getTime() >= (new Date().getTime() - 3600)) {
            login();
        }
    }

    private Resty authedClient() throws IOException, LoginException {
        checkIfAuthTicketIsValid();
        Resty r = new Resty();
        r.withHeader("Cookie", "PVEAuthCookie=" + authTicket);
        r.withHeader("CSRFPreventionToken", csrfPreventionToken);
        return r;
    }

    private JSONResource getJSONResource(String resource) throws IOException, LoginException {
        return authedClient().json(baseURL + resource);
    }

    public List<String> getNodes() throws IOException, LoginException, JSONException {
        List<String> res = new ArrayList<String>();
        JSONArray nodes = getJSONResource("nodes").toObject().getJSONArray("data");
        for (int i = 0; i < nodes.length(); i++) {
            res.add(nodes.getJSONObject(i).getString("node"));
        }
        return res;
    }

    public HashMap<String, Integer> getQemuMachines(String node) throws IOException, LoginException, JSONException {
        HashMap<String, Integer> res = new HashMap<String, Integer>();
        JSONArray qemuVMs = getJSONResource("nodes/" + node + "/qemu").toObject().getJSONArray("data");
        for (int i = 0; i < qemuVMs.length(); i++) {
            JSONObject vm = qemuVMs.getJSONObject(i);
            res.put(vm.getString("name"), vm.getInt("vmid"));
        }
        return res;
    }

    public List<String> getQemuMachineSnapshots(String node, Integer vmid)
            throws IOException, LoginException, JSONException {
        List<String> res = new ArrayList<String>();
        JSONArray snapshots = getJSONResource("nodes/" + node + "/qemu/" + vmid.toString() + "/snapshot")
                .toObject().getJSONArray("data");
        for (int i = 0; i < snapshots.length(); i++) {
            res.add(snapshots.getJSONObject(i).getString("name"));
        }
        return res;
    }

    public String rollbackQemuMachineSnapshot(String node, Integer vmid, String snapshotName)
            throws IOException, LoginException, JSONException {
        Resty r = authedClient();
        String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/snapshot/" + snapshotName + "/rollback";
        JSONResource response = r.json(resource, form(""));
        return response.toObject().getString("data");
    }

    public String stopQemuMachine(String node, Integer vmid) throws IOException, LoginException, JSONException {
        Resty r = authedClient();
        String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/status/stop";
        JSONResource response = r.json(resource, form(""));
        return response.toObject().getString("data");
    }

}
