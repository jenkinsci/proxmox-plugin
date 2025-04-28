package org.jenkinsci.plugins.proxmox.tools;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.jvnet.hudson.test.JenkinsMatchers.hasPlainText;

import hudson.model.Computer;
import hudson.model.Node.Mode;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.util.Secret;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Arrays;
import java.util.List;
import org.jenkinsci.plugins.proxmox.Datacenter;
import org.jenkinsci.plugins.proxmox.VirtualMachineLauncher.RevertPolicy;
import org.jenkinsci.plugins.proxmox.VirtualMachineSlave;
import org.jenkinsci.plugins.proxmox.VirtualMachineSlaveComputer;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void should_support_configuration_as_code(JenkinsConfiguredWithCodeRule r) {
        Datacenter cloud = (Datacenter) r.jenkins.clouds.get(0);
        assertThat(cloud.getHostname(), is("company-proxmox"));
        assertThat(cloud.getRealm(), is("pve"));
        assertThat(cloud.getUsername(), is("proxmox-user"));
        assertThat(cloud.getPassword(), hasPlainText("proxmox-pass"));
        assertThat(cloud.getIgnoreSSL(), is(true));

        List<Computer> computers = Arrays.asList(r.jenkins.getComputers());
        assertThat(computers, hasSize(2));
        assertThat(computers.get(1), instanceOf(VirtualMachineSlaveComputer.class));
        assertThat(computers.get(1).getNode(), instanceOf(VirtualMachineSlave.class));

        VirtualMachineSlave slave = (VirtualMachineSlave) computers.get(1).getNode();
        assertThat(slave.getLauncher(), instanceOf(JNLPLauncher.class));
        assertThat(slave.getRetentionStrategy(), instanceOf(RetentionStrategy.Demand.class));
        assertThat(slave.getDatacenterDescription(), is(cloud.getDatacenterDescription()));
        assertThat(slave.getDatacenterNode(), is("proxmox-node"));
        assertThat(slave.getLabelString(), is("proxmox-label"));
        assertThat(slave.getMode(), is(Mode.EXCLUSIVE));
        assertThat(slave.getNodeName(), is("proxmox-vm"));
        assertThat(slave.getNumExecutors(), is(1));
        assertThat(slave.getRemoteFS(), is("/home/jenkins"));
        assertThat(slave.getRevertPolicy(), is(RevertPolicy.BEFORE_JOB));
        assertThat(slave.getSnapshotName(), is("proxmox-snapshot"));
        assertThat(slave.getStartVM(), is(true));
        assertThat(slave.getStartupWaitingPeriodSeconds(), is(60));
        assertThat(slave.getVirtualMachineId(), is(42));
    }

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void should_support_configuration_export(JenkinsConfiguredWithCodeRule r) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Mapping cloud =
                getJenkinsRoot(context).get("clouds").asSequence().get(0).asMapping();

        String exported = toYamlString(cloud);
        Secret password = requireNonNull(
                Secret.decrypt(cloud.get("datacenter").asMapping().getScalarValue("password")));

        String expected = String.join(
                "\n",
                "datacenter:",
                "  hostname: \"company-proxmox\"",
                "  ignoreSSL: true",
                "  password: \"" + password.getEncryptedValue() + "\"",
                "  realm: \"pve\"",
                "  username: \"proxmox-user\"",
                "");

        assertThat(exported, is(expected));
    }
}
