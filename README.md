# enmasse-workshop
Workshop stuff about using EnMasse. In this workshop you will deploy EnMasse, Spark and an IoT
sensor simulator. You gain insight into deploying and operating an EnMasse cluster, and connect it
to a Spark cluster for analyzing the sensor data.

## Setting up

### Installing OpenShift

#### Downloading and installing minishift

In this workshop, we'll be using [minishift](https://github.com/minishift/minishift/) to run OpenShift locally on our laptops. Minishift supports all major OS platforms.  Go to https://github.com/minishift/minishift/releases/tag/v1.6.0 and select the download for your OS. 

#### Starting minishift

For this workshop, you need at least 4GB of RAM for your minishift instance since we're running both EnMasse and
Spark on a local OpenShift cluster.

```
minishift start --cpus 2 --memory 4096
```

Once this command completes, the OpenShift cluster should be ready to use.

### Exploring the console

Take a few minutes to explore the openshift console. Run `minishift dashboard`, which will launch
the OpenShift dashboard in your browser. You can login with username <b>developer</b> and password
<b>developer</b>. 

### Installing EnMasse

Go to [EnMasse downloads](https://github.com/EnMasseProject/enmasse/releases/latest) and download
the latest release (0.13.0 as of time of writing). Unpack the release:

```
tar xvf enmasse-0.13.0.tgz
```

The relase bundle contains OpenShift templates as well as a deployment script for deploying EnMasse.
We will use this script in this tutorial and have a look at its options to get a better idea of how
it works.

#### Deployment script

Run the deployment script with `-h` option

```
./enmasse-0.13.0/enmasse-deploy.sh -h
```



TODO:
   * Explore enmasse deploy options
   * Deploy EnMasse into its own namespace (multitenant mode?)
   * Go to EnMasse console, create addresses:
      * ...
   * Look at  logs for different components
   * Deploy grafana and configure metrics (?)
   * MQTT client simulator
      * Run publishers simulating IoT devices
      * Run subscribers as consumer application showing data
   * Real time streaming analytics using Spark Streaming ([EnMasse IoT demo](https://github.com/ppatierno/enmasse-iot-demo/blob/master/spark.md))
      * Deploy Spark cluster
      * Deploy Spark driver
   * ...
