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
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClientOptions;

import java.util.Properties;

/**
 * Client implementation for MQTT protocol
 */
public class MqttClient extends Client {

    private io.vertx.mqtt.MqttClient client;

    public MqttClient(String hostname, int port, Vertx vertx) {
        super(hostname, port, vertx);
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

        this.client = io.vertx.mqtt.MqttClient.create(vertx, options);
        this.client.connect(this.port, this.hostname, done -> {

            if (done.succeeded()) {
                // TODO
            } else {
                // TODO
            }
        });
    }

    @Override
    public void disconnet() {

        this.vertx.runOnContext(c -> {
            this.client.disconnect();
        });
    }

    @Override
    public void send(String address, String message, Handler<Void> sendCompletionHandler) {

        this.vertx.runOnContext(c -> {
            this.client.publish(address, Buffer.buffer(message), MqttQoS.AT_MOST_ONCE, false, false);
        });
    }
}
