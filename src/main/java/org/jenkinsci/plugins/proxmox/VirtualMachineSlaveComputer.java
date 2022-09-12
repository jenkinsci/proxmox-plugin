package org.jenkinsci.plugins.proxmox;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Slave;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;

public class VirtualMachineSlaveComputer extends SlaveComputer {

	private AtomicBoolean isRevertingSnapshot = new AtomicBoolean(false);

    public VirtualMachineSlaveComputer(Slave slave) {
        super(slave);
    }

    @Override
	public void tryReconnect() {
    	if(isRevertingSnapshot.get()) {
    		getListener().getLogger().println("INFO: trying to reconnect while snapshot revert - ignoring");
    		return;
    	}

		super.tryReconnect();
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
                	isRevertingSnapshot.set(true);

                	final Future<?> disconnectFuture = disconnect(OfflineCause.create(Messages._VirtualMachineSlaveComputer_disconnectBeforeSnapshotRevert()));
                	disconnectFuture.get();
                	getListener().getLogger().println("INFO: agent disconnected");

                	launcher.revertSnapshot(this, getListener());
                	getListener().getLogger().println("INFO: snapshot reverted");

                	launcher.launch(this, getListener());
                	getListener().getLogger().println("INFO: agent launched");
                } catch (IOException | InterruptedException e) {
                	getListener().getLogger().println("ERROR: Snapshot revert failed: " + e.getMessage());
                } catch (ExecutionException e) {
                	getListener().getLogger().println("ERROR: Exception while performing asynchronous disconnect: " + e.getMessage());
                } finally {
                	isRevertingSnapshot.set(false);
				}
            }
        }
    }
    
    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        return super._connect(forceReconnect);
    }

}
