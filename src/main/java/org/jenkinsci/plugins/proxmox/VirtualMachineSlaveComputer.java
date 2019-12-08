package org.jenkinsci.plugins.proxmox;

import java.io.IOException;
import java.util.concurrent.Future;

import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;

import hudson.Messages;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Slave;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;

public class VirtualMachineSlaveComputer extends SlaveComputer {

	/**
	 * The resource bundle reference
	 * 
	 */
	private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

    public VirtualMachineSlaveComputer(Slave slave) {
        super(slave);
    }

    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        super.taskAccepted(executor, task);

        final Node node = getNode();

        if (node instanceof VirtualMachineSlave) {
            final VirtualMachineSlave slave = (VirtualMachineSlave) node;
            final VirtualMachineLauncher launcher = (VirtualMachineLauncher) slave.getLauncher();

            if (launcher.isLaunchSupported() && (slave.getRevertPolicy() == VirtualMachineLauncher.RevertPolicy.BEFORE_JOB)) {
                try {
                  disconnect(OfflineCause.create(new Localizable(holder, "Disconnect before snapshot revert")));
                  launcher.revertSnapshot(this, getListener());
                  launcher.launch(this, getListener());
                } catch (IOException | InterruptedException e) {
                  getListener().getLogger().println("ERROR: Snapshot revert failed: " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        return super._connect(forceReconnect);
    }

}
