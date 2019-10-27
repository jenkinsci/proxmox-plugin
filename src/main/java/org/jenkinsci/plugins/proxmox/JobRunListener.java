package org.jenkinsci.plugins.proxmox;

import java.io.IOException;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

@Extension
public class JobRunListener extends RunListener<Run<?, ?> > {
  public JobRunListener (){
  }
  
  @Override
  public void onStarted(Run<?, ?> r, TaskListener listener) {
      super.onInitialize(r);
      
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
                launcher.launch(slaveComputer, listener);
              } catch (IOException | InterruptedException e) {
                listener.getLogger().println("ERROR: Snapshot revert failed: " + e.getMessage());
              }
          }
      }
  }
}
