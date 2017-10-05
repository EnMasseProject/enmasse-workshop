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
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;

import java.io.IOException;
import java.util.Properties;

/**
 * An heating device implementation
 */
public class HeatingDevice implements Device {

    private static final String HOSTNAME = "localhost";
    private static final int PORT = 5672;
    private static final String USERNAME = "device";
    private static final String PASSWORD = "password";
    private static final String TEMPERATURE_ADDRESS = "temperature";
    private static final int UPDATE_INTERVAL = 1000;

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

    @Override
    public void init(Properties config) {

        this.config = config;

        // initializing sensors and actuators
        this.dht22.init(null);
        this.valve.init(null);

        // getting hostname and port for client connection
        String hostname = this.config.getProperty(DeviceConfig.HOSTNAME);
        int port = Integer.valueOf(this.config.getProperty(DeviceConfig.PORT));

        this.client = new AmqpClient(hostname, port, this.vertx);
    }

    private void run() {

        String username = this.config.getProperty(DeviceConfig.USERNAME);
        String password = this.config.getProperty(DeviceConfig.PASSWORD);

        this.client.connect(username, password, this::connected);

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connected(AsyncResult<Client> ar) {

        if (ar.succeeded()) {

            int updateInterval = Integer.valueOf(this.config.getProperty(DeviceConfig.UPDATE_INTERVAL));
            String temperatureAddress = this.config.getProperty(DeviceConfig.TEMPERATURE_ADDRESS);
            this.vertx.setPeriodic(updateInterval, t -> {

                int temp = this.dht22.getTemperature();

                Client client = ar.result();
                client.send(temperatureAddress, String.valueOf(temp), v -> {
                    // TODO
                });

            });

        } else {
            // TODO
        }

    }

    public static void main(String[] args) {

        HeatingDevice heatingDevice = new HeatingDevice();

        Properties config = new Properties();
        config.setProperty(DeviceConfig.HOSTNAME, HOSTNAME);
        config.setProperty(DeviceConfig.PORT, String.valueOf(PORT));
        config.setProperty(DeviceConfig.USERNAME, USERNAME);
        config.setProperty(DeviceConfig.PASSWORD, PASSWORD);
        config.setProperty(DeviceConfig.TEMPERATURE_ADDRESS, TEMPERATURE_ADDRESS);
        config.setProperty(DeviceConfig.UPDATE_INTERVAL, String.valueOf(UPDATE_INTERVAL));

        heatingDevice.init(config);

        heatingDevice.run();
    }
}
