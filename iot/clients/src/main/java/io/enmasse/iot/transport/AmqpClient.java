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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.message.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Client implementation for AMQP protocol
 */
public class AmqpClient extends Client {

    private ProtonClient client;
    private ProtonConnection conn;
    private Map<String, ProtonSender> senders;

    public AmqpClient(String hostname, int port, Vertx vertx) {
        super(hostname, port, vertx);
    }

    @Override
    public void connect() {
        this.connect(null, null);
    }

    @Override
    public void connect(String username, String password) {

        this.client = ProtonClient.create(vertx);

        this.client.connect(this.hostname, this.port, username, password, done -> {

            if (done.succeeded()) {

                ProtonConnection conn = done.result();
                conn.open();

                this.senders = new HashMap<>();

            } else {
                // TODO
            }
        });
    }

    @Override
    public void send(String address, String message, Handler<Void> sendCompletionHandler) {

        ProtonSender sender = this.senders.get(address);
        if (sender == null) {

            sender = conn.createSender(address);
            sender.open();
            this.senders.put(address, sender);  
        }

        Message msg = ProtonHelper.message(message);
        msg.setAddress(address);

        if (sender.isOpen()) {

            sender.send(msg, delivery -> {
                // TODO
            });
        }
    }
}
