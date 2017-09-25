# enmasse-workshop
Workshop stuff about using EnMasse

## Setting up

### Installing OpenShift

#### Downloading and installing minishift

In this workshop, we'll be using [minishift](https://github.com/minishift/minishift/) to run OpenShift locally on our laptops. Minishift supports all major OS platforms.  Go to https://github.com/minishift/minishift/releases/tag/v1.6.0 and select the download for your OS. 

TODO:

   * Start minishift, explore some of the options like metrics
   * Download latest EnMasse release
   * Deploy EnMasse into its own namespace (multitenant mode?)
   * Go to EnMasse console, create addresses:
      * ...
   * Look at  logs for different components
   * Deploy grafana and configure metrics (?)
   * MQTT client simulator
      * Run publishers simulating IoT devices
      * Run subscribers as consumer application showing data
   * Real time streaming analytics using Spark Streaming
   * ...
