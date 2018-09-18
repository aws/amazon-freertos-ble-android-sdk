package com.amazon.aws.amazonfreertossdk.mqttproxy;

/**
 * This class represents the MQTT PUBLISH message.
 */
public class Publish {
    /**
     * MQTT message type.
     */
    private int type;
    /**
     * MQTT PUBLISH message topic.
     */
    private String topic;
    /**
     * MQTT message ID.
     */
    private int msgID;
    /**
     * MQTT PUBLISH message QOS.
     */
    private int qoS;
    /**
     * The data in the MQTT PUBLISH message.
     */
    private String payloadVal;

    public Publish(int type, String topic, int msgid, int qos, String payload) {
        this.type = type;
        this.topic = topic;
        this.msgID = msgid;
        this.qoS = qos;
        this.payloadVal = payload;
    }

    public String toString() {
        return String.format(" Publish message -> \n topic:%s\n msgID:%d\n qos:%d\n payload:%s",
                topic, msgID, qoS, payloadVal);
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payloadVal;
    }

    public int getMsgID() {
        return msgID;
    }

    public int getQos() {
        return qoS;
    }
}
