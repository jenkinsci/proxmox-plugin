package org.jenkinsci.plugins.proxmox;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Slave;
import hudson.model.Computer;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class VirtualMachineSlave extends Slave {

    private String datacenterDescription;
    private String datacenterNode;
    private String snapshotName;
    private Integer virtualMachineId;
    private int startupWaitingPeriodSeconds;

    @DataBoundConstructor
    public VirtualMachineSlave(String name, String nodeDescription, String remoteFS, String numExecutors,
                               Mode mode, String labelString, ComputerLauncher delegateLauncher,
                               RetentionStrategy retentionStrategy, List<? extends NodeProperty<?>> nodeProperties,
                               String datacenterDescription, String datacenterNode, Integer virtualMachineId,
                               String snapshotName, int startupWaitingPeriodSeconds)
            throws
            Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString,
                new VirtualMachineLauncher(delegateLauncher, datacenterDescription, datacenterNode, virtualMachineId,
                        snapshotName, startupWaitingPeriodSeconds),
                retentionStrategy, nodeProperties);
        this.datacenterDescription = datacenterDescription;
        this.datacenterNode = datacenterNode;
        this.virtualMachineId = virtualMachineId;
        this.snapshotName = snapshotName;
        this.startupWaitingPeriodSeconds = startupWaitingPeriodSeconds;
    }

    public String getDatacenterDescription() {
        return datacenterDescription;
    }

    public String getDatacenterNode() {
        return datacenterNode;
    }

    public Integer getVirtualMachineId() {
        return virtualMachineId;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public int getStartupWaitingPeriodSeconds() {
        return startupWaitingPeriodSeconds;
    }

    public ComputerLauncher getDelegateLauncher() {
        return ((VirtualMachineLauncher) getLauncher()).getDelegate();
    }

    @Override
    public Computer createComputer() {
        //TODO: Not sure if this is needed, could be able to use this to reset to snapshots
        //TODO: as a computer is required for a job.
        return new VirtualMachineSlaveComputer(this);
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {

        private String datacenterDescription;
        private String datacenterNode;
        private Integer virtualMachineId;
        private String snapshotName;

        public DescriptorImpl() {
            load();
        }

        public String getDisplayName() {
            return "Slave virtual machine running on a Proxmox datacenter.";
        }

        @Override
        public boolean isInstantiable() {
            return true;
        }

        public ListBoxModel doFillDatacenterDescriptionItems() {
            ListBoxModel items = new ListBoxModel();
            for (Cloud cloud : Hudson.getInstance().clouds) {
                if (cloud instanceof Datacenter) {
                    //TODO: Possibly add the `datacenterDescription` as the `displayName` and `value` (http://javadoc.jenkins-ci.org/hudson/util/ListBoxModel.html)
                    //Add by `display name` and then the `value`
                    items.add(((Datacenter) cloud).getHostname(), ((Datacenter) cloud).getDatacenterDescription());
                }
            }
            return items;
        }

        //TODO: Possibly replace these with `doFillDatacenterNodeItems` function
        public List<String> getDatacenterNodes(String datacenterDescription) {
            Datacenter datacenter = getDatacenterByDescription(datacenterDescription);
            if (datacenter != null) {
                return datacenter.getNodes();
            }
            return new ArrayList<String>();
        }

        public HashMap<String, Integer> getQemuVirtualMachines(String datacenterDescription, String datacenterNode) {
            Datacenter datacenter = getDatacenterByDescription(datacenterDescription);
            if (datacenter != null) {
                return datacenter.getQemuVirtualMachines(datacenterNode);
            }
            return new HashMap<String, Integer>();
        }

        public List<String> getQemuSnapshotNames(String datacenterDescription, String datacenterNode,
                                                 Integer virtualMachineId) {
            Datacenter datacenter = getDatacenterByDescription(datacenterDescription);
            if (datacenter != null) {
                return datacenter.getQemuVirtualMachineSnapshots(datacenterNode, virtualMachineId);
            }
            return new ArrayList<String>();
        }

        public String getDatacenterDescription() {
            return datacenterDescription;
        }

        public String getDatecenterNode() {
            return datacenterNode;
        }

        public Integer getVirtualMachineId() {
            return virtualMachineId;
        }

        public String getSnapshotName() {
            return snapshotName;
        }

        private Datacenter getDatacenterByDescription (String datacenterDescription) {
            if (datacenterDescription != null && !datacenterDescription.equals("")) {
                for (Cloud cloud : Hudson.getInstance().clouds) {
                    if (cloud instanceof Datacenter && ((Datacenter) cloud).getDatacenterDescription().equals(datacenterDescription)) {
                        return (Datacenter) cloud;
                    }
                }
            }
            return null;
        }
    }
}