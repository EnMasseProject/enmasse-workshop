/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.iot.device.impl;

import io.enmasse.iot.actuator.impl.Valve;
import io.enmasse.iot.device.Device;
import io.enmasse.iot.device.DeviceConfig;
import io.enmasse.iot.sensor.impl.DHT22;
import io.enmasse.iot.transport.AmqpClient;
import io.enmasse.iot.transport.Client;
import io.enmasse.iot.transport.MqttClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * An heating device implementation
 */
public class HeatingDevice implements Device {

    protected final Logger log = LoggerFactory.getLogger(HeatingDevice.class);

    private DHT22 dht22;
    private Valve valve;
    private Client client;

    private Properties config;

    private Vertx vertx;

    public HeatingDevice() {

        this.vertx = Vertx.vertx();

        this.dht22 = new DHT22();
        this.valve = new Valve();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(Properties config) {

        this.config = config;
        log.info("Init with config {}", config);

        // initializing sensors and actuators
        Properties configDht22 = new Properties();
        configDht22.setProperty("min", this.config.getProperty(DeviceConfig.DHT22_TEMPERATURE_MIN));
        configDht22.setProperty("max", this.config.getProperty(DeviceConfig.DHT22_TEMPERATURE_MAX));
        this.dht22.init(configDht22);
        this.valve.init(null);

        // getting hostname and port for client connection
        String hostname = this.config.getProperty(DeviceConfig.HOSTNAME);
        int port = Integer.valueOf(this.config.getProperty(DeviceConfig.PORT));

        try {
            // getting and creating the transport class to use
            Class transportClass = Class.forName(this.config.getProperty(DeviceConfig.TRANSPORT_CLASS));
            Constructor constructor = transportClass.getConstructor(String.class, int.class, Vertx.class);
            this.client = (Client) constructor.newInstance(hostname, port, this.vertx);
            log.info("Using {} as transport", transportClass);
        } catch (Exception e) {
            log.error("Transport class instantiation error ...", e);
            this.client = new AmqpClient(hostname, port, this.vertx);
            log.info("Using default {} as transport", AmqpClient.class);
        }
    }

    private void run() {

        String username = this.config.getProperty(DeviceConfig.USERNAME);
        String password = this.config.getProperty(DeviceConfig.PASSWORD);

        log.info("Connecting to the service ...");
        this.client.connect(username, password, this::connected);

        try {
            System.in.read();
            this.client.disconnect();
            this.vertx.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connected(AsyncResult<Client> done) {

        if (done.succeeded()) {

            log.info("Connected to the service");

            Client client = done.result();

            client.receivedHandler(messageDelivery -> {
                log.info("Received message on {} with payload {}",
                        messageDelivery.address(), messageDelivery.message());

                JsonObject object = new JsonObject(Buffer.buffer(messageDelivery.message()));
                String deviceId = object.getString("device-id");
                if (!deviceId.equals(this.config.getProperty(DeviceConfig.DEVICE_ID))) {
                    log.error("Received control message for some other device with id " + deviceId);
                } else {
                    String operation = object.getString("operation");
                    if ("open".equals(operation)) {
                        valve.open();
                    } else if ("close".equals(operation)) {
                        valve.close();
                    }
                }
            });

            client.receive(this.config.getProperty(DeviceConfig.CONTROL_ADDRESS));

            int updateInterval = Integer.valueOf(this.config.getProperty(DeviceConfig.UPDATE_INTERVAL));
            String temperatureAddress = this.config.getProperty(DeviceConfig.TEMPERATURE_ADDRESS);

            this.vertx.setPeriodic(updateInterval, t -> {

                int temperature = this.dht22.getTemperature();

                JsonObject json = new JsonObject();
                json.put("device-id", this.config.getProperty(DeviceConfig.DEVICE_ID));
                json.put("temperature", temperature);

                log.info("Sending temperature value = {} ...", temperature);
                client.send(temperatureAddress, json.toString().getBytes(), v -> {
                    log.info("... sent");
                });
            });

        } else {

            log.error("Error connecting to the service", done.cause());
        }

    }

    public static void main(String[] args) throws IOException {

        String configFile = System.getenv("DEVICE_PROPERTIES_FILE");
        if (configFile == null) {
            configFile = "device.properties";
        }

        HeatingDevice heatingDevice = new HeatingDevice();

        InputStream input = new FileInputStream(configFile);
        Properties config = new Properties();
        config.load(input);

        String controlAddress = String.format("%s/%s", config.getProperty(DeviceConfig.CONTROL_PREFIX), config.getProperty(DeviceConfig.DEVICE_ID));
        config.setProperty(DeviceConfig.CONTROL_ADDRESS, controlAddress);

        heatingDevice.init(config);

        heatingDevice.run();
    }
}
