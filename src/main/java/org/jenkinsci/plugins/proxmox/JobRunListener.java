package org.jenkinsci.plugins.proxmox;

import java.io.IOException;

import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;

import hudson.Extension;
import hudson.Messages;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.slaves.OfflineCause;

@Extension
public class JobRunListener extends RunListener<Run<?, ?> > {
  
  /**
   * The resource bundle reference
   * 
   */
  private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);
  
  public JobRunListener (){
  }
  
  @Override
  public void onStarted(Run<?, ?> r, TaskListener listener) {
      super.onStarted(r, listener);
      
      final Executor executor = r.getExecutor();
      
      if (executor == null) {
          return;
      }
      
      final Computer computer = executor.getOwner();
      final Node node = computer.getNode();

      if (node instanceof VirtualMachineSlave) {
          final VirtualMachineSlave slave = (VirtualMachineSlave) node;
          final VirtualMachineSlaveComputer slaveComputer = (VirtualMachineSlaveComputer) computer;
          final VirtualMachineLauncher launcher = (VirtualMachineLauncher) slave.getLauncher();

          if (launcher.isLaunchSupported()) {
              try {
                slaveComputer.disconnect(OfflineCause.create(new Localizable(holder, "Disconnect before snapshot revert")));
                launcher.revertSnapshot(slaveComputer, listener);
                launcher.launch(slaveComputer, listener);
              } catch (IOException | InterruptedException e) {
                listener.getLogger().println("ERROR: Snapshot revert failed: " + e.getMessage());
              }
          }
      }
  }
}
