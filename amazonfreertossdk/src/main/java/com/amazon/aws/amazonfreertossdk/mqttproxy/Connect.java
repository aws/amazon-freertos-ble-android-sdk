package com.amazon.aws.amazonfreertossdk.mqttproxy;

/**
 * This class represents the MQTT CONNECT message.
 */
public class Connect {
    /**
     * MQTT message type.
     */
    public int type;
    /**
     * MQTT client id.
     */
    public String clientID;
    /**
     * MQTT broker endpoint.
     */
    public String brokerEndpoint;
    /**
     * MQTT clean session.
     */
    public boolean cleanSession;
    public String toString() {
        return String.format(" Connect message -> \n clientID: %s\n endpoint: %s\n cleansession: %s",
                clientID, brokerEndpoint, (cleanSession? "true":"false") );
    }
}
