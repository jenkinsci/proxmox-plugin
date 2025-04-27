package org.jenkinsci.plugins.proxmox;

import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import jenkins.model.Jenkins;
import kong.unirest.json.JSONObject;
import org.jenkinsci.plugins.proxmox.pve2api.Connector;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Controls launching of Proxmox virtual machines.
 */
public class VirtualMachineLauncher extends DelegatingComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(VirtualMachineLauncher.class.getName());

    @Deprecated
    private transient ComputerLauncher delegate;

    @Deprecated
    private transient int WAIT_TIME_MS;

    private transient String datacenterDescription;
    private transient String datacenterNode;
    private transient Integer virtualMachineId;
    private transient String snapshotName;
    private transient Boolean startVM;
    private transient int waitingTimeSecs;

    public static enum RevertPolicy {
        AFTER_CONNECT("After connect to the virtual machine"),
        BEFORE_JOB("Before every job executing on the virtual machine");

        private final String label;

        private RevertPolicy(String policy) {
            this.label = policy;
        }

        public String getLabel() {
            return label;
        }
    }

    private final RevertPolicy revertPolicy;

    @DataBoundConstructor
    public VirtualMachineLauncher(
            ComputerLauncher launcher,
            String datacenterDescription,
            String datacenterNode,
            Integer virtualMachineId,
            String snapshotName,
            Boolean startVM,
            int waitingTimeSecs,
            RevertPolicy revertPolicy) {
        super(launcher);
        this.datacenterDescription = datacenterDescription;
        this.datacenterNode = datacenterNode;
        this.virtualMachineId = virtualMachineId;
        this.snapshotName = snapshotName;
        this.startVM = startVM;
        this.waitingTimeSecs = waitingTimeSecs;
        this.revertPolicy = revertPolicy;
    }

    /**
     * Migrates instances from the old parent class to the new parent class.
     * @return the deserialized instance.
     * @throws ObjectStreamException if something went wrong.
     */
    private Object readResolve() throws ObjectStreamException {
        if (delegate != null) {
            return new VirtualMachineLauncher(
                    delegate,
                    datacenterDescription,
                    datacenterNode,
                    virtualMachineId,
                    snapshotName,
                    startVM,
                    WAIT_TIME_MS / 1000,
                    revertPolicy);
        }
        return this;
    }

    /**
     * @return actual launcher
     * @deprecated use {@link #getLauncher()}
     */
    @Deprecated
    public ComputerLauncher getDelegate() {
        return launcher;
    }

    public Datacenter findDatacenterInstance() throws RuntimeException {
        if (datacenterDescription != null && virtualMachineId != null) {
            for (Cloud cloud : Jenkins.get().clouds) {
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
        // TODO: Add this into the settings for node setup
        boolean overrideLaunchSupported = launcher.isLaunchSupported();
        // Support launching for the JNLPLauncher, so the `launch` function gets called
        // and the VM can be reset to a snapshot.
        if (launcher instanceof JNLPLauncher) {
            overrideLaunchSupported = true;
        }
        return overrideLaunchSupported;
    }

    public void startSlaveIfNeeded(TaskListener taskListener) throws InterruptedException {
        String taskId = null;
        JSONObject taskStatus = null;
        try {
            Datacenter datacenter = findDatacenterInstance();
            Connector pve = datacenter.proxmoxInstance();
            Boolean isvmIdRunning = pve.isQemuMachineRunning(datacenterNode, virtualMachineId);
            if (!isvmIdRunning) {
                taskListener.getLogger().println("Starting virtual machine...");
                taskId = pve.startQemuMachine(datacenterNode, virtualMachineId);
                taskStatus = pve.waitForTaskToFinish(datacenterNode, taskId);
                taskListener.getLogger().println("Task finished! Status object: " + taskStatus.toString());
            }
        } catch (LoginException e) {
            taskListener.getLogger().println("ERROR: Login failed: " + e.getMessage());
        }
    }

    public void revertSnapshot(SlaveComputer slaveComputer, TaskListener taskListener) throws InterruptedException {
        String taskId = null;
        JSONObject taskStatus = null;

        try {
            Datacenter datacenter = findDatacenterInstance();
            Connector pve = datacenter.proxmoxInstance();

            if (!snapshotName.equals("current")) {
                taskListener
                        .getLogger()
                        .println("Virtual machine \"" + virtualMachineId + "\" (Name \""
                                + slaveComputer.getDisplayName() + "\") is being reverted...");
                // TODO: Check the status of this task (pass/fail) not just that its finished
                taskId = pve.rollbackQemuMachineSnapshot(datacenterNode, virtualMachineId, snapshotName);
                taskListener.getLogger().println("Proxmox returned: " + taskId);

                // Wait for the task to finish
                taskStatus = pve.waitForTaskToFinish(datacenterNode, taskId);
                taskListener.getLogger().println("Task finished! Status object: " + taskStatus.toString());
            }

            if (startVM) {
                startSlaveIfNeeded(taskListener);
            }

        } catch (LoginException e) {
            taskListener.getLogger().println("ERROR: Login failed: " + e.getMessage());
        }

        // Ignore the wait period for a JNLP agent as it connects back to the Jenkins instance.
        if (!(launcher instanceof JNLPLauncher)) {
            Thread.sleep(waitingTimeSecs * 1000);
        }
    }

    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener taskListener)
            throws IOException, InterruptedException {
        if (revertPolicy == RevertPolicy.AFTER_CONNECT) {
            revertSnapshot(slaveComputer, taskListener);
        } else {
            if (startVM) {
                startSlaveIfNeeded(taskListener);
            }
        }

        launcher.launch(slaveComputer, taskListener);
    }

    public void shutdown(SlaveComputer slaveComputer, TaskListener taskListener) {
        String taskId = null;
        JSONObject taskStatus = null;

        // try to gracefully shutdown the virtual machine
        try {
            taskListener
                    .getLogger()
                    .println("Virtual machine \"" + virtualMachineId + "\" (slave \"" + slaveComputer.getDisplayName()
                            + "\") is being shutdown.");
            Datacenter datacenter = findDatacenterInstance();
            Connector pve = datacenter.proxmoxInstance();
            taskId = pve.shutdownQemuMachine(datacenterNode, virtualMachineId);
            taskStatus = pve.waitForTaskToFinish(datacenterNode, taskId);
            if (!taskStatus.getString("exitstatus").equals("OK")) {
                // Graceful shutdown failed, so doing a stop.
                taskListener
                        .getLogger()
                        .println("Virtual machine \"" + virtualMachineId + "\" (slave \""
                                + slaveComputer.getDisplayName()
                                + "\") was not able to shutdown, doing a stop instead");
                taskId = pve.stopQemuMachine(datacenterNode, virtualMachineId);
                taskStatus = pve.waitForTaskToFinish(datacenterNode, taskId);
            }
            taskListener.getLogger().println("Task finished! Status object: " + taskStatus.toString());
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Waiting for task completion failed: " + e.getMessage());
        } catch (LoginException e) {
            LOGGER.log(Level.WARNING, "Login failed: " + e.getMessage());
        }
    }

    @Override
    public void beforeDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        if (revertPolicy == RevertPolicy.AFTER_CONNECT) shutdown(slaveComputer, taskListener);

        super.beforeDisconnect(slaveComputer, taskListener);
    }

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        throw new UnsupportedOperationException();
    }
}
