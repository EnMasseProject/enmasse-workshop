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

package io.enmasse.iot.device;

/**
 * Configuration properties for a device
 */
public class DeviceConfig {

    public static final String HOSTNAME = "service.hostname";
    public static final String PORT = "service.port";
    public static final String TEMPERATURE_ADDRESS = "service.temperature.address";
    public static final String CONTROL_PREFIX = "service.control.prefix";
    public static final String CONTROL_ADDRESS = "service.control.address";

    public static final String DEVICE_ID = "device.id";
    public static final String USERNAME = "device.username";
    public static final String PASSWORD = "device.password";
    public static final String UPDATE_INTERVAL = "device.update.interval";

    public static final String DHT22_TEMPERATURE_MIN = "device.dht22.temperature.min";
    public static final String DHT22_TEMPERATURE_MAX = "device.dht22.temperature.max";
}
