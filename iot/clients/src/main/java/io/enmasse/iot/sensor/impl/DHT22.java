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

package io.enmasse.iot.sensor.impl;

import io.enmasse.iot.sensor.HumiditySensor;
import io.enmasse.iot.sensor.TemperatureSensor;

import java.util.Properties;

/**
 * Simulated DHT22 temperature and humidity sensor
 * @see <a href="https://learn.adafruit.com/dht/overview">DHTxx family</a>
 */
public class DHT22 implements TemperatureSensor, HumiditySensor {

    @Override
    public int getHumidity() {
        return 0;
    }

    @Override
    public int getTemperature() {
        return 0;
    }

    @Override
    public void init(Properties config) {

    }
}
