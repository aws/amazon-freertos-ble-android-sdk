package com.amazon.aws.amazonfreertossdk.mqttproxy;

/**
 * This class represents the MQTT UNSUBSCRIBE message.
 */
public class Unsubscribe {
    /**
     * MQTT message type.
     */
    public int type;
    /**
     * Arrary of topics to unsubscribe.
     */
    public String[] topics;
    /**
     * MQTT message ID.
     */
    public int msgID;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UnSubscribe message: ");
        stringBuilder.append("\n    type: " + type);
        stringBuilder.append("\n    msgId: " + msgID);
        for (int i = 0; i < topics.length; i++) {
            stringBuilder.append("\n    topic: " + topics[i]);
        }
        return stringBuilder.toString();
    }
}
