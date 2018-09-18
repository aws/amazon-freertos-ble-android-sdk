package com.amazon.aws.amazonfreertossdk.mqttproxy;

/**
 * This class represents the MQTT proxy state. SDK sends this object to device to switch on/off
 * MQTT proxy.
 */
public class MqttProxyControl {
    /**
     * The state of MQTT proxy.
     */
    public int proxyState;
}
