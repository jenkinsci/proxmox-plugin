package org.jenkinsci.plugins.proxmox;

import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Controls launching of Proxmox virtual machines.
 */
public class VirtualMachineLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(VirtualMachineLauncher.class.getName());
    private ComputerLauncher delegate;
    private String datacenterDescription;
    private String virtualMachineName;
    private String snapshotName;
    private final int WAIT_TIME_MS;

    @DataBoundConstructor
    public VirtualMachineLauncher(ComputerLauncher delegate, String datacenterDescription, String virtualMachineName,
                                  String snapshotName, int waitingTimeSecs) {
        super();
        this.delegate = delegate;
        this.virtualMachineName = virtualMachineName;
        this.snapshotName = snapshotName;
        this.datacenterDescription = datacenterDescription;
        this.WAIT_TIME_MS = waitingTimeSecs*1000;
    }

    public ComputerLauncher getDelegate() {
        return delegate;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    @Override
    public boolean isLaunchSupported() {
        //TODO: Add this into the settings
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

        //TODO: Launch the `slaveComputer instance`
        taskListener.getLogger().println("Virtual machine \"" + virtualMachineName + "\" (slave title \"" + slaveComputer.getDisplayName() + "\") is to be started.");

        //TODO: Ignore the wait period for a JNLP agent as it connects back to the Jenkins instance.

        //Test code below
        Thread.sleep(10000);
        delegate.launch(slaveComputer, taskListener);
    }

    @Override
    public synchronized void afterDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        taskListener.getLogger().println("Virtual machine \"" + virtualMachineName + "\" (slave \"" + slaveComputer.getDisplayName() + "\") is to be shut down.");
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
