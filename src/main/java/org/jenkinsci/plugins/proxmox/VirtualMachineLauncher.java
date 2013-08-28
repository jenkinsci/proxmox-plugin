package org.jenkinsci.plugins.proxmox;

import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import net.elbandi.pve2api.Pve2Api;

import org.json.JSONException;

import javax.security.auth.login.LoginException;

/**
 * Controls launching of Proxmox virtual machines.
 */
public class VirtualMachineLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(VirtualMachineLauncher.class.getName());
    private ComputerLauncher delegate;
    private String datacenterDescription;
    private String datacenterNode;
    private Integer virtualMachineId;
    private String snapshotName;
    private final int WAIT_TIME_MS;

    @DataBoundConstructor
    public VirtualMachineLauncher(ComputerLauncher delegate, String datacenterDescription, String datacenterNode,
                                  Integer virtualMachineId, String snapshotName, int waitingTimeSecs) {
        super();
        this.delegate = delegate;
        this.datacenterDescription = datacenterDescription;
        this.datacenterNode = datacenterNode;
        this.virtualMachineId = virtualMachineId;
        this.snapshotName = snapshotName;
        this.WAIT_TIME_MS = waitingTimeSecs*1000;
    }

    public ComputerLauncher getDelegate() {
        return delegate;
    }

    public Datacenter findDatacenterInstance() throws RuntimeException {
        if (datacenterDescription != null && virtualMachineId != null) {
            for (Cloud cloud : Hudson.getInstance().clouds) {
                if (cloud instanceof Datacenter
                        && ((Datacenter) cloud).getDatacenterDescription().equals(datacenterDescription)) {
                    return (Datacenter) cloud;
                }
            }
        }
        throw new RuntimeException("Could not find the proxmox datacenter instance!");
    }

    @Override
    public boolean isLaunchSupported() {
        //TODO: Add this into the settings for node setup
        boolean overrideLaunchSupported = delegate.isLaunchSupported();
        //Support launching for the JNLPLauncher, so the `launch` function gets called
        //and the VM can be reset to a snapshot.
        if (delegate instanceof JNLPLauncher) {
            overrideLaunchSupported = true;
        }
        return overrideLaunchSupported;
    }

    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener taskListener) throws IOException, InterruptedException {
        taskListener.getLogger().println("Virtual machine \"" + virtualMachineId + "\" (slave title \"" + slaveComputer.getDisplayName() + "\") is to be started.");

        //TODO: Launch the `slaveComputer instance`

        try {
            Datacenter datacenter = findDatacenterInstance();
            Pve2Api pve = datacenter.proxmoxInstance();
            pve.rollbackQemu("proxmox", virtualMachineId, "ft_test");
        } catch (JSONException e) {
        } catch (LoginException e) {
        }

        //TODO: Ignore the wait period for a JNLP agent as it connects back to the Jenkins instance.

        //Test code below
        Thread.sleep(10000);
        delegate.launch(slaveComputer, taskListener);
    }

    @Override
    public synchronized void afterDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        taskListener.getLogger().println("Virtual machine \"" + virtualMachineId + "\" (slave \"" + slaveComputer.getDisplayName() + "\") is to be shut down.");
        delegate.afterDisconnect(slaveComputer, taskListener);
    }

    @Override
    public void beforeDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        //TODO: Shutdown (or stop) the virtual machine.
        delegate.beforeDisconnect(slaveComputer, taskListener);
    }

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        throw new UnsupportedOperationException();
    }
}
