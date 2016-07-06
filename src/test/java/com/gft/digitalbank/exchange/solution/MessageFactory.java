package com.gft.digitalbank.exchange.solution;

import com.gft.digitalbank.exchange.model.orders.MessageType;
import com.gft.digitalbank.exchange.model.orders.Side;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski@bravurasolutions.com on 2016-07-04.
 */
public class MessageFactory {

    public static JsonObject createModificationMessage(int orderId, int amount, int price, int timestamp, String broker) {
        JsonObject message = new JsonObject();
        message.addProperty("modifiedOrderId", orderId);
        message.addProperty("messageType", MessageType.MODIFICATION.toString());
        message.addProperty("amount", amount);
        message.addProperty("price", price);
        message.addProperty("timestamp", timestamp);
        message.addProperty("broker", broker);

        JsonObject details = new JsonObject();
        details.addProperty("amount", amount);
        details.addProperty("price", price);

        message.add("details", details);
        return message;
    }

    public static JsonObject createBuyMessage(int orderId, String product, int amount, int price, int timestamp, String broker, String client) {
        return createOrderMessage(orderId, product, amount, price, timestamp, broker, client, Side.BUY);
    }

    public static JsonObject createSellMessage(int orderId, String product, int amount, int price, int timestamp, String broker, String client) {
        return createOrderMessage(orderId, product, amount, price, timestamp, broker, client, Side.SELL);
    }

    private static JsonObject createOrderMessage(int orderId, String product, int amount, int price, int timestamp, String broker, String client, Side side) {
        JsonObject message = new JsonObject();
        message.addProperty("id", orderId);
        message.addProperty("messageType", MessageType.ORDER.toString());
        message.addProperty("side", side.toString());
        message.addProperty("product", product);
        message.addProperty("timestamp", timestamp);
        message.addProperty("broker", broker);
        message.addProperty("client", client);

        JsonObject details = new JsonObject();
        details.addProperty("amount", amount);
        details.addProperty("price", price);

        message.add("details", details);
        return message;
    }

    public static JsonObject createCancelMessage(int orderId, int timestamp, String broker) {
        JsonObject message = new JsonObject();
        message.addProperty("cancelledOrderId", orderId);
        message.addProperty("messageType", MessageType.CANCEL.toString());
        message.addProperty("timestamp", timestamp);
        message.addProperty("broker", broker);
        return message;
    }
}
