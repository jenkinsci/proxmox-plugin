package org.jenkinsci.plugins.proxmox;

import hudson.slaves.SlaveComputer;
import hudson.model.Slave;
import java.util.concurrent.Future;

public class VirtualMachineSlaveComputer extends SlaveComputer {

    public VirtualMachineSlaveComputer(Slave slave) {
        super(slave);
    }

    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        return super._connect(forceReconnect);
    }

}
