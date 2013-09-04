# jenkins-proxmox-plugin

## Description
Use Proxmox virtual machines as slaves in Jenkins.

## Requirements
**pve2-api-java**

Clone ``https://github.com/justnom/pve2-api-java`` and install ``mvn source:jar install``.

## Installing
 1. Clone this repo. 
 2. Run ``mvn clean package``. 
 3. Go to Jenkins in a web browser. 
 4. Click on *"Manage Jenkins"*, select *"Manage Plugins"*. 
 5. Click on the *"Advanced"* tab then upload the file `target/proxmox.hpi` under the *"Upload Plugin"* section.


