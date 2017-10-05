package io.enmasse.iot.controller.thermostat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Thermostat extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(Thermostat.class);

    private final String messagingHost;
    private final int messagingPort;
    private final String alarmAddress;
    private final String controlPrefix;
    private ProtonConnection connection;

    public Thermostat(String messagingHost, int messagingPort, String alarmAddress, String controlPrefix) {
        this.messagingHost = messagingHost;
        this.messagingPort = messagingPort;
        this.alarmAddress = alarmAddress;
        this.controlPrefix = controlPrefix;
    }

    @Override
    public void start(Future<Void> startPromise) {
        ProtonClient alarmClient = ProtonClient.create(vertx);
        alarmClient.connect(messagingHost, messagingPort, connection -> {
            if (connection.succeeded()) {
                log.info("Connected to {}:{}", messagingHost, messagingPort);
                ProtonConnection connectionHandle = connection.result();

                ProtonReceiver receiver = connectionHandle.createReceiver(alarmAddress);
                receiver.handler(this::handleNotification);
                receiver.openHandler(link -> {
                    if (link.succeeded()) {
                        startPromise.complete();
                    } else {
                        log.info("Error attaching to {}", alarmAddress, link.cause());
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
        JsonObject object = (JsonObject)((AmqpValue)message.getBody()).getValue();
        String deviceId = object.getString("deviceId");
        int temperature = object.getInteger("temperature");
        int threshold = object.getInteger("threshold");

        int newTemperature = calculateTemperature(temperature, threshold);
        sendNewTemperature(deviceId, newTemperature);
    }

    private void sendNewTemperature(String deviceId, int newTemperature) {
        ProtonSender sender = connection.createSender(controlPrefix + "/" + deviceId);

        JsonObject object = new JsonObject();
        object.put("deviceId", deviceId);
        object.put("operation", "setTemperature");
        object.put("value", newTemperature);
        Message controlMessage = Message.Factory.create();
        controlMessage.setBody(new AmqpValue(object.toString()));

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

    private int calculateTemperature(int temperature, int threshold) {
        // TODO: Make this smarter
        if (temperature > threshold) {
            return threshold - 2; // Due to inertia, lower the temp
        } else if (temperature < threshold) {
            return threshold + 2;
        } else {
            return temperature;
        }
    }

    public static void main(String [] args) {
        Map<String, String> env = System.getenv();
        String messagingHost = env.getOrDefault("MESSAGING_SERVICE_HOST", "localhost");
        int messagingPort = Integer.parseInt(env.getOrDefault("MESSAGING_SERVICE_PORT", "5672"));

        String alarmAddress = env.getOrDefault("ALARM_ADDRESS", "alarm");
        String controlPrefix = env.getOrDefault("COMMAND_CONTROL_PREFIX", "control");

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Thermostat(messagingHost, messagingPort, alarmAddress, controlPrefix));
    }
}
