## Service Agent
The official documentation for the Service Agent is on [Confluence](https://nkeu.atlassian.net/l/cp/EhVvMaiN).

### Table of Contents
* Features
* Installation
* Notifications

### Features
* Highly reliable decentralized monitoring for all of our servers and services
* Notifications that actually reach our team
* System for the execution of updates
* Compliance with our policies regarding updates and prohibited actions

### Installation
1. Install the latest Agent version from our GitHub releases
2. Upload the Agent ('MAG-v2022_9_1-dev.jar') to the ‘/home/monitoring/’ directory
3. Add a Linux user for the monitoring
4. Use systemctl services to manage the program lifecycle
5. Configure auto-restart in the service configuration
6. Start the service and follow the installation instructions

![service-agent-installation-guide](https://user-images.githubusercontent.com/50241630/198717219-35a235a0-50c4-43ec-aa50-abc9853abb7a.png)

### Notifications

#### Types of Notifications:
* Normal notifications: Agent / Service is unavailable for the first time
* Urgent notifications:  Agent / Service is still unavailable at the third try
* Critical notifications: Master Agent is unavailable

#### Notification channels:
* Team Control Panel
  * Browser push notifications
  * Personal notifications
  * Development dashboard alerts
  * Dashboard displays
* Mobile Apps
  * IOS
  * Android
* Discord App
  * Channel notifications
  * Private message notifications
* Desktop Application
  * Windows
  * Linux
  * Mac
* E-Mail
* Telephone
  * Phone calls
  * Short Message Service
