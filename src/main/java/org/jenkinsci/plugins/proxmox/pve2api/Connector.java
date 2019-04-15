package org.jenkinsci.plugins.proxmox.pve2api;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
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

import javax.net.ssl.*;
import javax.security.auth.login.LoginException;

public class Connector {
    protected String hostname;
    protected Integer port;
    protected String username;
    protected String realm;
    protected String password;
    protected Boolean ignoreSSL;
    protected String baseURL;

    private String authTicket;
    private Date authTicketIssuedTimestamp;
    private String csrfPreventionToken;

    private static SSLSocketFactory cachedSSLSocketFactory = null;
    private static HostnameVerifier cachedHostnameVerifier = null;

    private static void ignoreAllCerts() {
        if (cachedSSLSocketFactory == null)
            cachedSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        if (cachedHostnameVerifier == null)
            cachedHostnameVerifier  = HttpsURLConnection.getDefaultHostnameVerifier();

        TrustManager trm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            public X509Certificate[] getAcceptedIssuers() {    return null; }
        };

        HostnameVerifier hnv = new HostnameVerifier() {
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };

        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] { trm }, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hnv);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void resetCachedSSLHelperObjects() {
        if (cachedSSLSocketFactory != null)
            HttpsURLConnection.setDefaultSSLSocketFactory(cachedSSLSocketFactory);
        if (cachedHostnameVerifier != null)
            HttpsURLConnection.setDefaultHostnameVerifier(cachedHostnameVerifier);
    }

    public Connector(String hostname, String username, String realm, String password) {
        this(hostname, username, realm, password, false);
    }

    public Connector(String hostname, String username, String realm, String password, Boolean ignoreSSL) {
        //TODO: Split the hostname to check for a port.
        this.hostname = hostname;
        this.port = 8006;
        this.username = username;
        this.realm = realm;
        this.password = password;
        this.ignoreSSL = ignoreSSL;
        if (ignoreSSL)
            ignoreAllCerts();
        else
            resetCachedSSLHelperObjects();
        this.authTicketIssuedTimestamp = null;
        this.baseURL = "https://" + hostname + ":" + port.toString() + "/api2/json/";
    }

    public void login() throws IOException, LoginException {
        Resty r = new Resty();
        JSONResource authTickets = r.json(baseURL + "access/ticket",
                form("username=" + username + "@" + realm + "&password=" + password));
        try {
            authTicket = authTickets.get("data.ticket").toString();
            csrfPreventionToken = authTickets.get("data.CSRFPreventionToken").toString();
            authTicketIssuedTimestamp = new Date();
        } catch (Exception e) {
            throw new LoginException("Failed reading JSON response");
        }
    }

    public void checkIfAuthTicketIsValid() throws IOException, LoginException {
        //Authentication ticket has a lifetime of 2 hours, so login again when it expires
        if (authTicketIssuedTimestamp == null
                || authTicketIssuedTimestamp.getTime() <= (new Date().getTime() - (120 * 60 * 1000))) {
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

    public JSONObject getTaskStatus(String node, String taskId) throws IOException, LoginException, JSONException {
        JSONResource response = getJSONResource("nodes/" + node + "/tasks/" + taskId + "/status");
        return response.toObject().getJSONObject("data");
    }
    
    public JSONObject getQemuMachineStatus(String node, Integer vmid) throws IOException, LoginException, JSONException {
      JSONResource response = getJSONResource("nodes/" + node + "/qemu/" + vmid + "/status/current");
      return response.toObject().getJSONObject("data");
    }
    
    public Boolean isQemuMachineRunning(String node, Integer vmid) throws IOException, LoginException, JSONException {
      JSONObject QemuMachineStatus = null;
      Boolean isRunning = true;
      QemuMachineStatus = getQemuMachineStatus(node, vmid);
      isRunning = (QemuMachineStatus.getString("status").equals("running"));
      return isRunning;
  }

    public JSONObject waitForTaskToFinish(String node, String taskId) throws IOException, LoginException, JSONException {
        JSONObject lastTaskStatus = null;
        Boolean isRunning = true;
        while (isRunning) {
            lastTaskStatus = getTaskStatus(node, taskId);
            isRunning = (lastTaskStatus.getString("status").equals("running"));
        }
        return lastTaskStatus;
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
        JSONResource response = r.json(baseURL + resource, form(""));
        return response.toObject().getString("data");
    }

    public String startQemuMachine(String node, Integer vmid) throws IOException, LoginException, JSONException {
        Resty r = authedClient();
        String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/status/start";
        JSONResource response = r.json(baseURL + resource, form(""));
        return response.toObject().getString("data");
    }

    public String stopQemuMachine(String node, Integer vmid) throws IOException, LoginException, JSONException {
        Resty r = authedClient();
        String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/status/stop";
        JSONResource response = r.json(baseURL + resource, form(""));
        return response.toObject().getString("data");
    }
    
    public String shutdownQemuMachine(String node, Integer vmid) throws IOException, LoginException, JSONException {
      Resty r = authedClient();
      String resource = "nodes/" + node + "/qemu/" + vmid.toString() + "/status/shutdown";
      JSONResource response = r.json(baseURL + resource, form(""));
      return response.toObject().getString("data");
  }

}
