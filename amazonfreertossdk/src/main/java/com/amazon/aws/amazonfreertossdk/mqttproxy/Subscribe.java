package com.amazon.aws.amazonfreertossdk.mqttproxy;

/**
 * This class represents the MQTT SUBSCRIBE message.
 */
public class Subscribe {
    /**
     * MQTT message type.
     */
    public int type;
    /**
     * Arrary of topics to subscribe to.
     */
    public String[] topics;
    /**
     * MQTT message ID.
     */
    public int msgID;
    /**
     * Arrary of QOS for each subscribe topic.
     */
    public int[] qoSs;
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Subscribe message: ");
        stringBuilder.append("\n    type: " + type);
        stringBuilder.append("\n    msgId: " + msgID);
        for (int i = 0; i < topics.length; i++) {
            stringBuilder.append("\n    topic: " + topics[i] + ", qos: " + qoSs[i]);
        }
        return stringBuilder.toString();
    }
}
