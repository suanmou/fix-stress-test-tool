package com.fixstress;

import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;
import quickfix.fix44.OrderStatusRequest;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FixMessageGenerator {
    private TestConfig config;
    private String symbol = "AAPL";
    
    public FixMessageGenerator(TestConfig config) {
        this.config = config;
    }
    
    public Message createMessage(SessionID sessionId) {
        int messageType = determineMessageType();
        
        switch (messageType) {
            case 1: // NewOrderSingle
                return createNewOrderSingle(sessionId);
            case 2: // OrderCancelRequest
                return createOrderCancelRequest(sessionId);
            case 3: // OrderStatusRequest
                return createOrderStatusRequest(sessionId);
            default:
                return createNewOrderSingle(sessionId);
        }
    }
    
    private int determineMessageType() {
        int random = ThreadLocalRandom.current().nextInt(1, 101);
        
        if (random <= config.getNewOrderRatio()) {
            return 1; // NewOrderSingle
        } else if (random <= config.getNewOrderRatio() + config.getOrderCancelRatio()) {
            return 2; // OrderCancelRequest
        } else {
            return 3; // OrderStatusRequest
        }
    }
    
    private NewOrderSingle createNewOrderSingle(SessionID sessionId) {
        NewOrderSingle order = new NewOrderSingle();
        
        // 设置消息头
        order.getHeader().setField(new MsgType("D"));
        order.getHeader().setField(new SenderCompID(config.getSenderId()));
        order.getHeader().setField(new TargetCompID(config.getTargetId()));
        
        // 设置消息体
        String clOrdId = UUID.randomUUID().toString().substring(0, 20);
        order.setField(new ClOrdID(clOrdId));
        order.setField(new Symbol(symbol));
        order.setField(new Side(Side.BUY));
        order.setField(new TransactTime(new java.util.Date()));
        order.setField(new OrdType(OrdType.MARKET));
        
        // 随机数量
        double quantity = ThreadLocalRandom.current().nextDouble(1, 1000);
        order.setField(new OrderQty(quantity));
        
        // 添加额外字段以达到配置的消息大小
        addExtraFields(order, config.getMsgSize());
        
        return order;
    }
    
    private OrderCancelRequest createOrderCancelRequest(SessionID sessionId) {
        OrderCancelRequest cancelRequest = new OrderCancelRequest();
        
        // 设置消息头
        cancelRequest.getHeader().setField(new MsgType("F"));
        cancelRequest.getHeader().setField(new SenderCompID(config.getSenderId()));
        cancelRequest.getHeader().setField(new TargetCompID(config.getTargetId()));
        
        // 设置消息体
        String clOrdId = UUID.randomUUID().toString().substring(0, 20);
        String origClOrdId = "ORIG_" + UUID.randomUUID().toString().substring(0, 15);
        
        cancelRequest.setField(new ClOrdID(clOrdId));
        cancelRequest.setField(new OrigClOrdID(origClOrdId));
        cancelRequest.setField(new Symbol(symbol));
        cancelRequest.setField(new Side(Side.BUY));
        cancelRequest.setField(new TransactTime(new java.util.Date()));
        
        // 添加额外字段以达到配置的消息大小
        addExtraFields(cancelRequest, config.getMsgSize());
        
        return cancelRequest;
    }
    
    private OrderStatusRequest createOrderStatusRequest(SessionID sessionId) {
        OrderStatusRequest statusRequest = new OrderStatusRequest();
        
        // 设置消息头
        statusRequest.getHeader().setField(new MsgType("H"));
        statusRequest.getHeader().setField(new SenderCompID(config.getSenderId()));
        statusRequest.getHeader().setField(new TargetCompID(config.getTargetId()));
        
        // 设置消息体
        String clOrdId = UUID.randomUUID().toString().substring(0, 20);
        statusRequest.setField(new ClOrdID(clOrdId));
        statusRequest.setField(new Symbol(symbol));
        statusRequest.setField(new Side(Side.BUY));
        
        // 添加额外字段以达到配置的消息大小
        addExtraFields(statusRequest, config.getMsgSize());
        
        return statusRequest;
    }
    
    private void addExtraFields(Message message, int targetSize) {
        // 计算当前消息大小
        int currentSize = message.toString().length();
        
        // 如果需要，添加额外字段以达到目标大小
        if (currentSize < targetSize) {
            int extraCharsNeeded = targetSize - currentSize;
            String extraData = generateRandomString(extraCharsNeeded);
            message.setField(new StringField(9999, extraData)); // 使用自定义字段
        }
    }
    
    private String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = ThreadLocalRandom.current().nextInt(characters.length());
            result.append(characters.charAt(index));
        }
        
        return result.toString();
    }
}