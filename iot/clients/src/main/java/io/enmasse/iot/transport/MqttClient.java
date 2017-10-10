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

package io.enmasse.iot.transport;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.mqtt.MqttClientOptions;

import java.util.Properties;

/**
 * Client implementation for MQTT protocol
 */
public class MqttClient extends Client {

    private io.vertx.mqtt.MqttClient client;

    public MqttClient(String hostname, int port, String serverCert, Vertx vertx) {
        super(hostname, port, serverCert, vertx);
    }

    @Override
    public void init(Properties config) {

    }

    @Override
    public void connect(Handler<AsyncResult<Client>> connectHandler) {
        this.connect(null, null, connectHandler);
    }

    @Override
    public void connect(String username, String password, Handler<AsyncResult<Client>> connectHandler) {

        MqttClientOptions options =
                new MqttClientOptions()
                .setUsername(username)
                .setPassword(password);

        if (this.serverCert != null && !this.serverCert.isEmpty()) {
            options.setSsl(true)
                    .setHostnameVerificationAlgorithm("")
                    .setPemTrustOptions(new PemTrustOptions().addCertPath(this.serverCert));
        }

        this.client = io.vertx.mqtt.MqttClient.create(vertx, options);
        this.client.connect(this.port, this.hostname, done -> {

            if (done.succeeded()) {

                log.info("Connected to {}:{}", this.hostname, this.port);

                this.client.publishHandler(m -> {

                    MessageDelivery messageDelivery = new MessageDelivery(m.topicName(), m.payload().getBytes());
                    this.receivedHandler.handle(messageDelivery);
                });

                connectHandler.handle(Future.succeededFuture(this));

            } else {

                log.error("Error connecting to the service", done.cause());
                connectHandler.handle(Future.failedFuture(done.cause()));
            }
        });
    }

    @Override
    public void disconnect() {

        this.vertx.runOnContext(c -> {
            this.client.disconnect();
            log.info("Disconnected");
        });
    }

    @Override
    public void send(String address, byte[] data, Handler<String> sendCompletionHandler) {

        this.vertx.runOnContext(c -> {
            this.client.publish(address, Buffer.buffer(data), MqttQoS.AT_MOST_ONCE, false, false, done -> {

                if (done.succeeded()) {
                    if (sendCompletionHandler != null) {
                        sendCompletionHandler.handle(done.result().toString());
                    }
                }
            });
        });
    }

    @Override
    public void receive(String address) {

        this.vertx.runOnContext(c -> {
            this.client.subscribe(address, MqttQoS.AT_MOST_ONCE.value());
        });
    }
}
