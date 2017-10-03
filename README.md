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

In this workshop, we will deploy using the standard (keycloak) authentication service, use the `enmasse` namespace, and tell it to deploy to our minishift cluster.

```
./enmasse-0.13.0/enmasse-deploy.sh -a standard -n enmasse -m https://$(minishift ip):8443
```

#### Startup

You can observe the state of the EnMasse cluster using `oc get pods -n enmasse`. When all the pods are in the `Running` state, the cluster is ready. While waiting, go to the OpenShift console.

In the OpenShift console, you can see the different deployments for the various EnMasse components. You can go into each pod and look at the logs. If we go to the address controller log, you can see that its creating a 'default' address space.

In EnMasse, you have the concepts of address spaces and addresses.

* Each address space can be accessed through a single (per protocol) connection and are isolated
  from each other.

TODO:
   * Go to EnMasse console, create addresses:
      * explain queues, topics, anycast, multicast
   * Look at  logs for different components
   * Deploy grafana and configure metrics (?)
   * MQTT client simulator
      * Run publishers simulating IoT devices
      * Run subscribers as consumer application showing data
   * Real time streaming analytics using Spark Streaming ([EnMasse IoT demo](https://github.com/ppatierno/enmasse-iot-demo/blob/master/spark.md))
      * Deploy Spark cluster
      * Deploy Spark driver
   * ...
