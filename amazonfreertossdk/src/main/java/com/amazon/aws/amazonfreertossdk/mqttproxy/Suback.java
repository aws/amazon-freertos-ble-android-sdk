package com.amazon.aws.amazonfreertossdk.mqttproxy;

/**
 * This class represents the MQTT SUBACK message.
 */
public class Suback {
    /**
     * MQTT message type.
     */
    public int type;
    /**
     * MQTT message ID.
     */
    public int msgID;
    /**
     * MQTT SUBACK status. This is set to the QOS number in the corresponding MQTT SUBSCRIBE
     * message, to which this SUBACK is acknowledging.
     */
    public int status;
}
