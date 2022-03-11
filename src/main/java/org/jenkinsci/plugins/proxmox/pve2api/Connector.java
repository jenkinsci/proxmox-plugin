package org.jenkinsci.plugins.proxmox.pve2api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.LoginException;
import java.net.URI;
import java.net.URISyntaxException;

import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.json.JSONObject;
import kong.unirest.json.JSONArray;

import hudson.util.Secret;

public class Connector {

    public static final long WAIT_TIME_MS = 1000;

    protected Integer port;
    protected String username;
    protected String realm;
    protected Secret password;
    protected String baseURL;

    private String authTicket;
    private Date authTicketIssuedTimestamp;
    private String csrfPreventionToken;
	private UnirestInstance unirest;
	
	private static final Logger LOGGER = Logger.getLogger(Connector.class.getName());

    public Connector(String hostname, String username, String realm, Secret password) {
        this(hostname, username, realm, password, false);
    }

    public Connector(String hostname, String username, String realm, Secret password, Boolean ignoreSSL) {
        this.port = 8006;
		// Parse hostname for port information
		try {
			URI uri = new URI("https://" + hostname);
			if (uri.getPort() != -1) {
				hostname = uri.getHost();
				port = uri.getPort();
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
        this.username = username;
        this.realm = realm;
        this.password = password;
		
		this.unirest = Unirest.spawnInstance();
		unirest.config().verifySsl(!ignoreSSL).reset();
        
        this.authTicketIssuedTimestamp = null;
        this.baseURL = "https://" + hostname + ":" + port.toString() + "/api2/json/";
    }

    public void login() throws LoginException {
        JSONObject authTickets = unirest.post(baseURL + "access/ticket")
			.field("username", username + "@" + realm)
			.field("password", password.getPlainText())
			.asJson()
			.getBody()
			.getObject();
        try {
			JSONObject data = authTickets.getJSONObject("data");
            authTicket = data.get("ticket").toString();
            csrfPreventionToken = data.get("CSRFPreventionToken").toString();
            authTicketIssuedTimestamp = new Date();
        } catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed reading JSON response", e);
            throw new LoginException("Failed reading JSON response");
        }
    }

    public void checkIfAuthTicketIsValid() throws LoginException {
        //Authentication ticket has a lifetime of 2 hours, so login again when it expires
        if (authTicketIssuedTimestamp == null
                || authTicketIssuedTimestamp.getTime() <= (new Date().getTime() - (120 * 60 * 1000))) {
            login();
        }
    }
	
	private HttpResponse<JsonNode> JSONResource(HttpRequest req) throws LoginException {
		checkIfAuthTicketIsValid();
		return req.header("Cookie", "PVEAuthCookie=" + authTicket)
        .header("CSRFPreventionToken", csrfPreventionToken)
		.asJson();
	}

    private JsonNode getJSONResource(String apiUrl) throws LoginException {
        return JSONResource(unirest.get(baseURL + apiUrl))
		.getBody();
    }
	
	private JsonNode postJSONResource(String apiUrl, String body) throws LoginException {
        return JSONResource(unirest.post(baseURL + apiUrl)
			.header("Content-Type", "application/x-www-form-urlencoded")
			.body(body))
		.getBody();
    }

    public List<String> getNodes() throws LoginException {
        List<String> res = new ArrayList<String>();
        JSONArray nodes = getJSONResource("nodes").getObject().getJSONArray("data");
        for (int i = 0; i < nodes.length(); i++) {
            res.add(nodes.getJSONObject(i).getString("node"));
        }
        return res;
    }

    public JSONObject getTaskStatus(String node, String taskId) throws LoginException {
        JsonNode response = getJSONResource("nodes/" + node + "/tasks/" + taskId + "/status");
        return response.getObject().getJSONObject("data");
    }
    
    public JSONObject getQemuMachineStatus(String node, Integer vmid) throws LoginException {
      JsonNode response = getJSONResource("nodes/" + node + "/qemu/" + vmid + "/status/current");
      return response.getObject().getJSONObject("data");
    }
    
    public Boolean isQemuMachineRunning(String node, Integer vmid) throws LoginException {
      JSONObject QemuMachineStatus = null;
      Boolean isRunning = true;
      QemuMachineStatus = getQemuMachineStatus(node, vmid);
      isRunning = (QemuMachineStatus.getString("status").equals("running"));
      return isRunning;
  }

    public JSONObject waitForTaskToFinish(String node, String taskId) throws LoginException, InterruptedException {
        JSONObject lastTaskStatus = null;
        Boolean isRunning = true;
        while (isRunning) {
            lastTaskStatus = getTaskStatus(node, taskId);
            isRunning = (lastTaskStatus.getString("status").equals("running"));
            if (isRunning) {
                Thread.sleep(WAIT_TIME_MS);
            }
        }
        return lastTaskStatus;
    }

    public HashMap<String, Integer> getQemuMachines(String node) throws LoginException {
        HashMap<String, Integer> res = new HashMap<String, Integer>();
        JSONArray qemuVMs = getJSONResource("nodes/" + node + "/qemu").getObject().getJSONArray("data");
        for (int i = 0; i < qemuVMs.length(); i++) {
            JSONObject vm = qemuVMs.getJSONObject(i);
            res.put(vm.getString("name"), vm.getInt("vmid"));
        }
        return res;
    }

    public List<String> getQemuMachineSnapshots(String node, Integer vmid) throws LoginException {
        List<String> res = new ArrayList<String>();
        JSONArray snapshots = getJSONResource("nodes/" + node + "/qemu/" + vmid.toString() + "/snapshot")
                .getObject().getJSONArray("data");
        for (int i = 0; i < snapshots.length(); i++) {
            res.add(snapshots.getJSONObject(i).getString("name"));
        }
        return res;
    }

    public String rollbackQemuMachineSnapshot(String node, Integer vmid, String snapshotName) throws LoginException {
        return postJSONResource("nodes/" + node + "/qemu/" + vmid.toString() + "/snapshot/" + snapshotName + "/rollback", "")
			.getObject()
			.getString("data");
    }

    public String startQemuMachine(String node, Integer vmid) throws LoginException {
        return postJSONResource("nodes/" + node + "/qemu/" + vmid.toString() + "/status/start", "")
			.getObject()
			.getString("data");
    }

    public String stopQemuMachine(String node, Integer vmid) throws LoginException {
        return postJSONResource("nodes/" + node + "/qemu/" + vmid.toString() + "/status/stop", "")
			.getObject()
			.getString("data");
    }
    
    public String shutdownQemuMachine(String node, Integer vmid) throws LoginException {
		return postJSONResource("nodes/" + node + "/qemu/" + vmid.toString() + "/status/shutdown", "")
			.getObject()
			.getString("data");
    }
	
	 protected void finalize() {
		 unirest.shutDown();
	 }

}
