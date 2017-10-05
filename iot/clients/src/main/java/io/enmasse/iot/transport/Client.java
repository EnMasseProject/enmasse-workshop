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

import java.util.Properties;

/**
 * Base class for different transport protocol clients (AMQP, MQTT, ...)
 */
public abstract class Client {

    protected final String hostname;
    protected final int port;
    protected final Vertx vertx;
    protected Handler<String> receivedHandler;
    protected Handler<Void> sendCompletionHandler;

    /**
     * Constructor
     *
     * @param hostname  hostname to connect to
     * @param port  host port to connect to
     * @param vertx Vert.x instance used for the client
     */
    public Client(String hostname, int port, Vertx vertx) {
        this.hostname = hostname;
        this.port = port;
        this.vertx = vertx;
    }

    /**
     * Client initialization
     *
     * @param config    properties bag with client configuration parameters
     */
    public abstract void init(Properties config);

    /**
     * Connect to the remote system
     *
     * @param connectHandler    handler called when connection is established (or not)
     */
    public abstract void connect(Handler<AsyncResult<Client>> connectHandler);

    /**
     * Connect to the remote system with username/password credentials
     *
     * @param username  username
     * @param password  password
     * @param connectHandler    handler called when connection is established (or not)
     */
    public abstract void connect(String username, String password, Handler<AsyncResult<Client>> connectHandler);

    /**
     * Disconnect from the remote system
     */
    public abstract void disconnet();

    /**
     * Send a message to the remote system
     *
     * @param address   address to send the message
     * @param message   message to send
     * @param sendCompletionHandler handler to call on sent completion
     */
    public abstract void send(String address, String message, Handler<Void> sendCompletionHandler);

    /**
     * Set the handler for incoming messages
     *
     * @param handler   handler to call when a message is received
     * @return  current client instance
     */
    public Client receivedHandler(Handler<String> handler) {
        this.receivedHandler = handler;
        return this;
    }

}
