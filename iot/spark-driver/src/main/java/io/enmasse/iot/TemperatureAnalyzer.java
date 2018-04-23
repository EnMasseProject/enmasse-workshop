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

package io.enmasse.iot;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.apache.spark.SparkConf;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.amqp.AMQPUtils;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.Some;
import scala.Tuple2;

/**
 * Sample Spark driver for getting temperature values from sensor
 * analyzing them in real team as a stream providing max value in
 * the latest 5 secs
 */
public class TemperatureAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(TemperatureAnalyzer.class);

    private static final String APP_NAME = System.getenv("SPARK_APP"); //"TemperatureAnalyzer";
    private static final Duration BATCH_DURATION = new Duration(1000);

    private static final String CHECKPOINT_DIR = "/tmp/spark-streaming-amqp";

    private static String host = "localhost";
    private static int port = 5672;
    private static String username = null;
    private static String password = null;
    private static String temperatureAddress = "temperature";
    private static String maxAddress = "max";

    public static void main(String[] args) throws InterruptedException {

        // getting AMQP messaging service connection information
        String messagingServiceHost = System.getenv("MESSAGING_SERVICE_HOST");
        if (messagingServiceHost != null) {
            host = messagingServiceHost;
        }
        String messagingServicePort = System.getenv("MESSAGING_SERVICE_PORT");
        if (messagingServicePort != null) {
            port = Integer.valueOf(messagingServicePort);
        }
        log.info("AMQP messaging service hostname {}:{}", host, port);

        // getting credentials for authentication
        username = System.getenv("MESSAGING_USERNAME");
        password = System.getenv("MESSAGING_PASSWORD");
        log.info("Credentials {}/{}", username, password);

        JavaStreamingContext ssc = JavaStreamingContext.getOrCreate(CHECKPOINT_DIR, TemperatureAnalyzer::createStreamingContext);

        ssc.start();
        ssc.awaitTermination();
    }

    private static JavaStreamingContext createStreamingContext() {

        SparkConf conf = new SparkConf().setAppName(APP_NAME);
        log.info("appName = {}", APP_NAME);
        //conf.setMaster("local[2]");
        conf.set("spark.streaming.receiver.writeAheadLog.enable", "true");

        JavaStreamingContext ssc = new JavaStreamingContext(conf, BATCH_DURATION);
        ssc.checkpoint(CHECKPOINT_DIR);

        JavaReceiverInputDStream<DeviceTemperature> receiveStream =
                AMQPUtils.createStream(ssc, host, port,
                        Option.apply(username), Option.apply(password), temperatureAddress,
                        message -> {

                            Section section = message.getBody();
                            if (section instanceof AmqpValue) {
                                Object value = ((AmqpValue) section).getValue();
                                DeviceTemperature deviceTemperature = DeviceTemperature.fromJson(value.toString());
                                return new Some<>(deviceTemperature);
                            } else if (section instanceof Data) {
                                Binary data = ((Data)section).getValue();
                                DeviceTemperature deviceTemperature = DeviceTemperature.fromJson(new String(data.getArray(), "UTF-8"));
                                return new Some<>(deviceTemperature);
                            } else {
                                return null;
                            }

                        }, StorageLevel.MEMORY_ONLY());

        // from a stream with DeviceTemperature instace to a pair stream with key = device-id, value = temperature
        JavaPairDStream<String, Integer> temperaturesByDevice = receiveStream.mapToPair(deviceTemperature -> {
            return new Tuple2<>(deviceTemperature.deviceId(), deviceTemperature.temperature());
        });

        // reducing the pair stream by key (device-id) for getting max temperature value
        JavaPairDStream<String, Integer> max = temperaturesByDevice.reduceByKeyAndWindow(
                (a,b) -> {

                    if (a > b)
                        return a;
                    else
                        return b;

                }, new Duration(5000), new Duration(5000));

        //max.print();

        Broadcast<String> messagingHost = ssc.sparkContext().broadcast(host);
        Broadcast<Integer> messagingPort = ssc.sparkContext().broadcast(port);
        Broadcast<String> driverUsername = ssc.sparkContext().broadcast(username);
        Broadcast<String> driverPassword = ssc.sparkContext().broadcast(password);

        max.foreachRDD(rdd -> {

            rdd.foreach(record -> {

                // building a DeviceTemperature instance from the pair key = device-id, value = temperature
                DeviceTemperature deviceTemperature = new DeviceTemperature(record._1(), record._2());

                Vertx vertx = Vertx.vertx();
                ProtonClient client = ProtonClient.create(vertx);

                log.info("Connecting to messaging ...");
                client.connect(messagingHost.value(), messagingPort.value(),
                        driverUsername.value(), driverPassword.value(), done -> {

                            if (done.succeeded()) {

                                log.info("... connected to {}:{}", messagingHost.value(), messagingPort.getValue());

                                ProtonConnection connection = done.result();
                                connection.open();

                                ProtonSender maxSender = connection.createSender(maxAddress);
                                maxSender.open();

                                Message message = ProtonHelper.message();
                                message.setAddress(maxAddress);
                                message.setBody(new Data(new Binary(deviceTemperature.toJson().toString().getBytes())));

                                log.info("Sending {} to max address ...", deviceTemperature);
                                maxSender.send(message, maxDelivery -> {

                                    log.info("... message sent");
                                    maxSender.close();

                                    connection.close();
                                    vertx.close();

                                });

                            } else {

                                log.error("Error on AMQP connection for sending", done.cause());
                                vertx.close();
                            }

                        });
            });
        });

        return ssc;
    }
}
