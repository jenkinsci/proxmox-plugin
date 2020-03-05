# Jenkins Proxmox Plugin

Use Proxmox virtual machines as slaves in Jenkins

[![Proxmox Plugin](https://img.shields.io/jenkins/plugin/v/proxmox.svg)](https://plugins.jenkins.io/proxmox)
[![ChangeLog](https://img.shields.io/github/release/jenkinsci/proxmox-plugin.svg?label=changelog)](https://github.com/jenkinsci/proxmox-plugin/releases/latest)
[![Installs](https://img.shields.io/jenkins/plugin/i/proxmox.svg?color=blue)](https://plugins.jenkins.io/proxmox)
[![License](https://img.shields.io/github/license/jenkinsci/proxmox-plugin.svg)](LICENSE)
[![Build Status](https://ci.jenkins.io/job/Plugins/job/proxmox-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/proxmox-plugin/job/master/)

## Description

This plugin allows the use of Proxmox virtual machines as slaves in Jenkins.

## Limitations

-   Only Qemu virtual machines supported (at the moment).
-   No option to avoid rolling back to a snapshot on slave start up.
-   No checking on virtual machine ready state/errors during rollback.

## Configuration

#### Datacenter cloud

To add a new Proxmox datacenter cloud, click on "Manage Jenkins" then
"Configure System". In the "Cloud" section click "Add cloud" and select
"Datacenter".

#### Virtual machine slaves

To add slaves click on "Manage Jenkins" then "Manage Nodes". Select the
node type "Slave virtual machine running on a Proxmox datacenter." and
enter a name for the node.

## Manually Installing
 1. Clone this repo.
 2. Run ``mvn clean package``. 
 3. Go to Jenkins in a web browser.
 4. Click on *"Manage Jenkins"*, select *"Manage Plugins"*. 
 5. Click on the *"Advanced"* tab then upload the file `target/proxmox.hpi` under the *"Upload Plugin"* section.
 
To run directly a Jenkins test instance with the plugin, run ``mvn hpi:run``.


## ChangLog
-   For recent versions, see [GitHub Releases](https://github.com/jenkinsci/proxmox-plugin/releases)
-   For versions 0.2.1 and older, see the [Wiki page](https://wiki.jenkins.io/display/JENKINS/Proxmox+Plugin)

