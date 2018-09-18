package com.amazon.aws.amazonfreertossdk.mqttproxy;

/**
 * This class represents the MQTT CONNACK message.
 */
public class Connack {
    /**
     * MQTT message type.
     */
    public int type;
    /**
     * The MQTT connection status defined in {@code MqttConnectionState} enum.
     */
    public int status;
}
