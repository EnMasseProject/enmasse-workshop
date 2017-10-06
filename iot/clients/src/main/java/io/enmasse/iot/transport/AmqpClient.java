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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Client implementation for AMQP protocol
 */
public class AmqpClient extends Client {

    private ProtonClient client;
    private ProtonConnection connection;
    private Map<String, ProtonSender> senders;
    private Map<String, ProtonReceiver> receivers;

    public AmqpClient(String hostname, int port, Vertx vertx) {
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

        this.client = ProtonClient.create(vertx);

        this.client.connect(this.hostname, this.port, username, password, done -> {

            if (done.succeeded()) {

                log.info("Connected to {}:{}", this.hostname, this.port);

                this.connection = done.result();
                this.connection.open();

                if (this.senders != null) {
                    this.senders.clear();
                } else {
                    this.senders = new HashMap<>();
                }

                if (this.receivers != null) {
                    this.receivers.clear();
                } else {
                    this.receivers = new HashMap<>();
                }

            } else {

                log.error("Error connecting to the service", done.cause());
            }
        });
    }

    @Override
    public void disconnect() {

        this.vertx.runOnContext(c -> {
            for (ProtonSender sender: this.senders.values()) {
                sender.close();
            }
            for (ProtonReceiver receiver: this.receivers.values()) {
                receiver.close();
            }
            this.senders.clear();
            this.receivers.clear();
            this.connection.close();

            log.info("Disconnected");
        });
    }

    @Override
    public void send(String address, String message, Handler<Void> sendCompletionHandler) {

        this.vertx.runOnContext(c -> {
            ProtonSender sender = this.senders.get(address);
            if (sender == null) {

                sender = this.connection.createSender(address);
                sender.open();
                this.senders.put(address, sender);
            }

            Message msg = ProtonHelper.message(message);
            msg.setAddress(address);

            if (sender.isOpen()) {

                sender.send(msg, delivery -> {

                    this.sendCompletionHandler.handle(null);
                });
            }
        });
    }

    @Override
    public void receive(String address) {

        this.vertx.runOnContext(c -> {
            if (!this.receivers.containsKey(address)) {

                final ProtonReceiver receiver = this.connection.createReceiver(address);
                receiver.handler((delivery, message) -> {
                   this.receiverHandler(receiver, delivery, message);
                });
                receiver.open();
            }
        });
    }

    private void receiverHandler(ProtonReceiver receiver, ProtonDelivery delivery, Message message) {

        MessageDelivery messageDelivery =
                new MessageDelivery(receiver.getSource().getAddress(),
                        ((AmqpValue)message.getBody()).getValue().toString());

        this.receivedHandler.handle(messageDelivery);
    }
}
