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

import java.io.Serializable;

/**
 * Class representing a message from a device containing its id and temperature
 */
public class DeviceTemperature implements Serializable {

    private final String deviceId;
    private final int temperature;

    /**
     * Constructor
     *
     * @param deviceId  deviceId
     * @param temperature   temperature
     */
    public DeviceTemperature(String deviceId, int temperature) {
        this.deviceId = deviceId;
        this.temperature = temperature;
    }

    /**
     * @return  deviceId
     */
    public String deviceId() {
        return this.deviceId;
    }

    /**
     * @return  temperature
     */
    public int temperature() {
        return this.temperature;
    }

    @Override
    public String toString() {
        return "DeviceTemperature(deviceId=" +
                this.deviceId + ",temperature=" +
                this.temperature + ")";
    }
}
