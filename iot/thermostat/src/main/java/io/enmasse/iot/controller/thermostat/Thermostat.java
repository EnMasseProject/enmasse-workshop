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

package io.enmasse.iot.controller.thermostat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Thermostat extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(Thermostat.class);

    private final String messagingHost;
    private final int messagingPort;
    private final String username;
    private final String password;
    private final String notificationAddress;
    private final String controlPrefix;
    private final int minTemp;
    private final int maxTemp;
    private ProtonConnection connection;

    public Thermostat(String messagingHost, int messagingPort, String username, String password, String notificationAddress, String controlPrefix, int minTemp, int maxTemp) {
        this.messagingHost = messagingHost;
        this.messagingPort = messagingPort;
        this.username = username;
        this.password = password;
        this.notificationAddress = notificationAddress;
        this.controlPrefix = controlPrefix;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
    }

    @Override
    public void start(Future<Void> startPromise) {
        ProtonClient alarmClient = ProtonClient.create(vertx);
        alarmClient.connect(messagingHost, messagingPort, username, password, connection -> {
            if (connection.succeeded()) {
                log.info("Connected to {}:{}", messagingHost, messagingPort);
                ProtonConnection connectionHandle = connection.result();

                ProtonReceiver receiver = connectionHandle.createReceiver(notificationAddress);
                receiver.handler(this::handleNotification);
                receiver.openHandler(link -> {
                    if (link.succeeded()) {
                        startPromise.complete();
                    } else {
                        log.info("Error attaching to {}", notificationAddress, link.cause());
                        startPromise.fail(link.cause());
                    }
                });
                receiver.open();
                connectionHandle.open();
                this.connection = connectionHandle;
            } else {
                log.info("Error connecting to {}:{}", messagingHost, messagingPort);
                startPromise.fail(connection.cause());
            }
        });
    }

    private void handleNotification(ProtonDelivery delivery, Message message) {
        JsonObject object = new JsonObject(Buffer.buffer(((Data) message.getBody()).getValue().getArray()));
        String deviceId = object.getString("device-id");
        int temperature = object.getInteger("temperature");

        adjustTemperature(deviceId, temperature);
    }

    private void adjustTemperature(String deviceId, int temperature) {
        if (temperature < minTemp) {
            sendCommand(deviceId, "open");
        } else if (temperature > maxTemp) {
            sendCommand(deviceId, "close");
        }
    }

    private void sendCommand(String deviceId, String command) {
        ProtonSender sender = connection.createSender(controlPrefix + "/" + deviceId);

        JsonObject object = new JsonObject();
        object.put("device-id", deviceId);
        object.put("operation", command);
        Message controlMessage = Message.Factory.create();

        controlMessage.setBody(new Data(new Binary(object.toBuffer().getBytes())));

        sender.openHandler(link -> {
            if (link.succeeded()) {
                sender.send(controlMessage, delivery -> sender.close());
            } else {
                log.info("Error sending control message to {}", deviceId);
            }
            sender.close();
        });
        sender.closeHandler(link -> {
            sender.close();
        });
        sender.open();
    }

    public static void main(String [] args) throws IOException {
        Properties properties = loadProperties("config.properties");
        String messagingHost = properties.getProperty("service.hostname", "messaging.enmasse.svc");
        int messagingPort = Integer.parseInt(properties.getProperty("service.port", "5672"));
        String username = properties.getProperty("service.username", "test");
        String password = properties.getProperty("service.password", "test");

        String maxAddress = properties.getProperty("address.max", "max");
        String controlPrefix = properties.getProperty("address.control.prefix", "control");

        int minTemp = Integer.parseInt(properties.getProperty("control.temperature.min", "15"));
        int maxTemp = Integer.parseInt(properties.getProperty("control.temperature.max", "25"));

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Thermostat(messagingHost, messagingPort, username, password, maxAddress, controlPrefix, minTemp, maxTemp));
    }

    private static Properties loadProperties(String resource) throws IOException {
        Properties properties = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream(resource);
        properties.load(stream);
        return properties;
    }
}
