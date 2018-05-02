# EnMasse Workshop
In this workshop you will deploy [EnMasse](http://enmasse.io/), [Apache Spark](https://spark.apache.org/) and an IoT sensors simulator.
You gain insight into deploying and operating an EnMasse cluster, and connect it to a Spark cluster for analyzing the sensors data.

## Prerequisites

This tutorial can be run from scratch where you install OpenShift, EnMasse and Spark. You
might also have an environment setup for you with these components, in which case you can skip the
parts marked optional. When installing from scratch, tutorial requires [Ansible](www.ansible.org) to deploy components to OpenShift.

To build the java code, you need [Maven](https://maven.apache.org/) already installed on the machine.  If you don't have that, there is the [official installation guide](https://maven.apache.org/install.html) for doing that. Finally, the [OpenShift](https://www.openshift.org) client tools is used for several operations.

## Overview

In this workshop we will be working with 5 different components:

* An EnMasse messaging service
* A Spark application containing the analytics code
* A Thermostat application performing command & control of devices
* One or more IoT device simulators

The first will be deployed directly to OpenShift and may be already setup for you. The spark and thermostat applications will be built and
deployed to OpenShift from your laptop, and the device IoT simulator will be running locally on your laptop.

![deployment](images/demo_deployment.png)

## (Optional) Installing OpenShift

### Downloading and installing minishift

If you don't have an OpenShift cluster available, you can use [minishift](https://github.com/minishift/minishift/) to run OpenShift locally on your laptop. Minishift supports all major OS platforms.  Go to https://github.com/minishift/minishift/releases and select the latest version and the download for your OS.

### Starting minishift

For this workshop, we are going to use the Service Catalog which is part of the experimental features group at time of writing and for this reason needs to be explicitly enabled.

```
export MINISHIFT_ENABLE_EXPERIMENTAL=y
```

In this wat, the `--service-catalog` flag can be used on starting minishift in order to enable the Service Catalog.
Then you need at least 4GB of RAM for your minishift instance since we're running both EnMasse and Spark on a local OpenShift cluster.

```
minishift start --cpus 2 --memory 4096 --service-catalog true
```

Once this command completes, the OpenShift cluster should be ready to use.

In order to run the Ansible playbook used for deploying EnMasse, the logged user needs admin rights. It should be satisfied by the cluster administrator but using minishift it's not true from the beginning. For this reason, it's needed to assign `cluster-admin` rights to the user (i.e. "developer").

```
oc login -u system:admin
oc adm policy add-cluster-role-to-user cluster-admin developer
oc login -u developer -p developer
```

### Exploring the console

Take a few minutes to familiarize yourself with the OpenShift console. If you use minishift, you can run `minishift dashboard` which will open a window in your web browser. With minishift, you can login with username <b>developer</b> and password <b>developer</b>.

### Getting OC tools

In order to execute commands against the OpenShift cluster, an `oc` client tool is needed.
Go to [OpenShift Origin client tools releases](https://github.com/openshift/origin/releases/) and download
the latest stable release (3.7.2 as of time of writing). Unpack the release:

```
tar xvf openshift-origin-client-tools-v3.7.2-282e43f-linux-64bit.tar.gz
```

Then add the folder with the `oc` tools to the `PATH` :

```
PATH=$PATH:openshift-origin-client-tools-v3.7.2-282e43f-linux-64bit.tar.gz
```

## (Optional) Installing EnMasse

EnMasse is an open source messaging platform, with focus on scalability and performance. EnMasse can run on your own infrastructure or in the cloud, and simplifies the deployment of messaging infrastructure.

For this workshop, all messages will flow through EnMasse in some way.

The EnMasse version used in this workshop can be found in the `enmasse` directory. We will use an [Ansible](www.ansible.org) playbook to install EnMasse and have a look at its options.

### Running the playbook

This workshop will use the following [playbook](enmasse/ansible/playbooks/openshift/workshop.yml):

```
- hosts: localhost
  vars:
    namespace: enmasse-workshop
    multitenant: true
    enable_rbac: true
    service_catalog: true
    keycloak_admin_password: admin
    authentication_services:
      - standard
  roles:
    - enmasse
```

This playbook instructs Ansible to install EnMasse to the `enmasse-workshop` namespace in OpenShift.  We will use the service catalog integration to make it easy to provision the messaging service. We will also use [Keycloak](www.keycloak.org) for authentication. If your OpenShift cluster is on a public network, please change the `keycloak_admin_password` to what you prefer.

You can modify the settings to your liking, but the rest of the workshop will assume the above being set.

To install EnMasse, first log in to your OpenShift cluster, then run the playbook:

```
oc login -u developer -p developer https://localhost:8443 
ansible-playbook enmasse/ansible/playbooks/openshift/workshop.yml
```

### Startup

You can observe the state of the EnMasse cluster using `oc get pods -n enmasse-workshop`. When all the pods are in the `Running` state, the cluster is ready. While waiting, go to the OpenShift console.

In the OpenShift console, you can see the different deployments for the various EnMasse components. You can go into each pod and look at the logs. If we go to the address controller log, you can see that its creating a 'default' address space.

## IoT Application

Now that the cluster has been set up, its time to work on the example application. First, login to
the OpenShift console and create a workspace for your project:

![OpenShift1](images/openshiftconsole1.png)
![OpenShift1](images/openshiftconsole2.png)


### Provisioning messaging

We now provision messaging infrastructure to use with the application.

In the OpenShift Service Catalog overview, select the "EnMasse (standard)" service.

![Provision1](images/messagingprovision1.png)

Select the 'unlimited-standard' plan.

![Provision2](images/messagingprovision2.png)

Select the project previously created and enter a name for the address space.

![Provision3](images/messagingprovision3.png)

Skip creating the binding.

![Provision4](images/messagingprovision4.png)

The address space will be provisioned and may take a few minutes. Jump to the project page.

![Provision5](images/messagingprovision5.png)

Once the provisioning is complete you should be able to see the dashboard link which we will later use to access the messaging console and create the addresses we need for the workshop.

![Provision6](images/messagingprovision6.png)

But first, and introduction to some EnMasse concepts.

### Address spaces and addresses

In EnMasse, you have the concepts of address spaces and addresses. When you provision a messaging
service like above, you effectively create an address space.

An address space is a group of addresses that can be accessed through a single connection (per
protocol). This means that clients connected to the endpoints of an address space can send messages
to or receive messages from any address it is authorized to send messages to or receive messages
from within that address space. An address space can support multiple protocols, which is defined by
the address space type.

Each messaging service provisioned in the service catalog creates a new address space. Conceptually,
an address space may share messaging infrastructure with other address spaces.

An address is part of an address space and represents a destination used for sending and receiving
messages. An address has a type, which defines the semantics of sending messages to and receiving
messages from that address. An address also has a plan, which determines the amount of resources
provisioned to support the address.

In the 'standard' address space, we have 4 types of addresses.

   * **multicast** : 'direct' one-to-many
   * **anycast** : 'direct' peer-2-peer
   * **queue** : queue
   * **topic** : pub/sub

### Creating addresses for this workshop

Click on the dashboard link to get to the messaging console:

![Provision6](images/messagingprovision6.png)

You will be redirected to a login screen. Click the 'OpenShift' button to login with your OpenShift
credentials:

![Login1](images/messaginglogin1.png)

On the side of the login form, you can see a button named "OpenShift". Click on that to authenticate your user using your OpenShift credentials.

![Login2](images/messaginglogin2.png)

Once logged in, click on the "Addresses" link and click "create" to create addresses.

![Create1](images/create1.png)

Create the following addresses:

   * _temperature_ : type topic - used by devices to report temperature

![CreateTemp1](images/createtemp1.png)
![CreateTemp2](images/createtemp2.png)


   * _max_ : type anycast - used by Spark driver to report the max temperature

![CreateMax1](images/createmax1.png)
![CreateMax2](images/createmax2.png)


   * _control/deviceX_ : type topic - used to send control messages to devices. Per-device control messages will be sent to control/$device-id

![Createdev1](images/createdev1.png)
![Createdev2](images/createdev2.png)

Once the addresses has been created, they should all be marked ready by the green 'tick':

![AddrOverview1](images/addressoverview.png)

### Authentication and Authorization

In this workshop we aim to setup a secure-by-default IoT solution, so we need to define the 
applications and what addresses they need to access. Before we create the bindings we need, lets
define the mapping:

* _deviceX_ :
  * recv: control/deviceX
  * send: temperature
* _spark_ : 
  * recv: temperature
  * send: max
* _thermostat_ :
  * recv: max
  * send: control

We will create the bindings to each of the application as we deploy them.

### Deploying the "Temperature Analyzer" Spark application

The spark application is composed of 2 parts:

* Apache Spark cluster
* Apache Spark driver

#### Deploying a Spark Cluster

First, login to the cluster using the command line:

```
oc login https://localhost:8443 -u user1
oc project user1
```

To deploy the spark cluster, use the [template](../../spark/cluster-template.yaml) provided in this tutorial:

```
oc process -f spark/cluster-template.yaml MASTER_NAME=spark-master | oc create -f -
```

This will deploy the spark cluster which may take a while. In your project overview you should see
the deployments:

![Spark1](images/spark1.png)

#### Deploying the Spark driver

The `iot/spark-driver` directory provides the Spark Streaming driver application and a Docker image for running the related Spark driver inside the cluster. The spark-driver is deployed by building and running it on the OpenShift cluster.  The spark-driver uses the [fabric8-maven-plugin](https://github.com/fabric8io/fabric8-maven-plugin) to create a docker image, an OpenShift deployment config, and deploy the spark-driver into OpenShift.

To deploy the spark driver:

```
cd iot/spark-driver
mvn clean package fabric8:resource fabric8:build fabric8:deploy -Dspark.master.host=spark-master.user1.svc
```

This command will package the application and build a Docker image deployed to OpenShift. You can
watch the status by looking at the build:

![Spark2](images/spark2.png)

Once the driver has been deployed, you should see it in the project overview:

![Spark3](images/spark3.png)

We now need to create a binding with the permissions we defined above. Click on "Create binding" to open the dialog to create a binding.

![SparkBinding1](images/sparkbinding1.png)

Set `sendAddresses` to `max` and `recvAddresses` to `temperature`.

![SparkBinding2](images/sparkbinding2.png)
![SparkBinding3](images/sparkbinding3.png)

Go to the secret that was created and click "Add to application". 

![SparkBinding4](images/sparkbinding4.png)
![SparkBinding5](images/sparkbinding5.png)


This will allow you modify your application deployment to mount the secret so that the example application can use it. Select the option to mount it and enter `/etc/app-credentials` as the mount point.

![SparkBinding6](images/sparkbinding6.png)

The spark-driver will now redeploy and read the credentials from the binding.

![SparkBinding7](images/sparkbinding7.png)

### Deploying the "Thermostat" application

The thermostat application uses the [fabric8-maven-plugin](https://github.com/fabric8io/fabric8-maven-plugin) to create a docker image, an OpenShift deployment config, and deploy the thermostat into OpenShift.

To build the application as a Docker image and deploy it to the OpenShift cluster:

```
cd iot/thermostat
mvn package fabric8:resource fabric8:build fabric8:deploy -Dfabric8.mode=openshift
```

The thermostat will be deployed to the OpenShift cluster. You can see the builds by going to the builds menu again:

![Thermostat1](images/thermostat1.png)

Eventually, the thermostat is deployed:

![Thermostat2](images/thermostat2.png)

Once the thermostat has been deployed, we need to create a binding with the permissions we defined above. Click on "Create binding" to open the dialog to create a binding. Set `sendAddresses` to `control*` and `recvAddresses` to `max`.

![Thermostat3](images/thermostat3.png)
![Thermostat4](images/thermostat4.png)

Go to the secret that was created and click "Add to application". 

![Thermostat5](images/thermostat5.png)
![Thermostat6](images/thermostat6.png)

This will allow you modify your application deployment to mount the secret so that the example application can use it. Select the option to mount it and enter `/etc/app-credentials` as the mount point.

![Thermostat7](images/thermostat7.png)

The thermostat will now redeploy and read the credentials from the binding.

![Thermostat8](images/thermostat8.png)

### Running the IoT simulated devices

Heating simulated devices are provided for simulating data sent to the IoT system and receiving messages.
The devices supports two protocols, AMQP and MQTT, which are configurable.
The Heating device application :

* get temperature values from a simulated DHT22 temperature sensor sending them to the _temperature_ address periodically
* receive commands for opening/closing a simulated valve on the _control/$device-id_ address

The console application can be configured using a `device.properties` file which provides following parameters :

* _service.hostname_ : hostname of the EnMasse messaging/mqtt service to connect (for AMQP or MQTT)
* _service.port_ : port of the EnMasse messaging service to connect
* _service.temperature.address_ : address on which temperature values will be sent (should not be changed from the _temperature_ value)
* _service.control.prefix_ : prefix for defining the control address for receiving command (should not be changed from the _control_ value)
* _device.id_ : device identifier
* _device.username_ : device username (from binding) for EnMasse authentication
* _device.password_ : device password (from binding) for EnMasse authentication
* _device.update.interval_ : periodic interval for sending temperature values
* _device.transport.class_ : transport class to use in terms of protocol. Possible values are _io.enmasse.iot.transport.AmqpClient_ for AMQP and _io.enmasse.iot.transport.MqttClient_ for MQTT
* _device.transport.ssl.servercert_ : server certificate file path for accessing EnMasse using a TLS connection
* _device.dht22.temperature.min_ : minimum temperature provided by the simulated DHT22 sensor
* _device.dht22.temperature.max_ : maximum temperature provided by the simulated DHT22 sensor

#### Configuring device

First, create another binding for the device.

![Device1](images/device1.png)

This time, we want the device to read control messages for its address. We also want it to be able
to send to the temperature address. Most importantly, we want to get hold of the external hostnames
that the device can connect to, so make sure 'externalAccess' is set.

![Device2](images/device2.png)

Once created, view the device secret.

![Device3](images/device3.png)

Reveal the secrets!

![Device4](images/device4.png)

Capture the values for the `externalMessagingHost` and `externalMessagingPort` and configure the `service.hostname` and `service.port` fields in `iot/clients/src/main/resources/device-amqp.properties`. For MQTT, use the values `externalMqttHost` and `externalMqttPort` and write them to `iot/clients/src/main/resources/device-mqtt.properties` instead.

Store the value of the `messagingCert.pem` field in a local file and update the
`device.transport.ssl.servercert` field in `iot/clients/src/main/resources/device-amqp.properties`.  The `messagingCert` and `mqttCert` fields contains the certificates needed by the AMQP and MQTT clients respectively.

![Device4](images/device4.png)

Finally, copy the values for the `username` and `password` into `device.username` and
`device.password` in the device properties file.

![Device5](images/device5.png)

An example final configuration:

```
# service related configuration
service.hostname=messaging-enmasse-user1.192.168.1.220.nip.io
service.port=443
service.temperature.address=temperature
service.control.prefix=control
# device specific configuration
device.id=device1
device.username=user-8fc43b14-98ab-4f70-940b-2fcbb681bdf7
device.password=qpNWm/zEc+H5V5oadG9jh7WwkySZXRTOEDy/MtgqrlQ=
device.update.interval=1000
device.transport.class=io.enmasse.iot.transport.AmqpClient
device.transport.ssl.servercert=messagingCert.pem
# device sensors specific configuration
device.dht22.temperature.min=20
device.dht22.temperature.max=30
```

#### Run device using pre-built JARs

The provided `heating-device.jar` can be used for starting a simulated heating device with the following command for AMQP (replace with device-mqtt.properties for MQTT):

```
java -jar iot/clients/jar/heating-device.jar iot/clients/src/main/resources/device-amqp.properties
```

The console application needs only one argument which is the path to the `device.properties` file with the device configuration.

Example output:

![Device6](images/device6.png)

#### Run device using Maven

In order to run the `HeatingDevice` application you can use the Maven `exec` plugin with the following command from the `clients` directory.

```
cd iot/clients
mvn exec:java -Dexec.mainClass=io.enmasse.iot.device.impl.HeatingDevice -Dexec.args=<path-to-device-properties-file>
```

You can run such command more times in order to start more than one devices (using different Keycloak users and device-id for them). The provided `device-amqp.properties` and `device-mqtt.properties` files can be used as starting point for AMQP and MQTT device configuration.

