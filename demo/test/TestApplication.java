package com.finance.fix.tester;

import quickfix.*;
import quickfix.field.TestReqID;
import quickfix.fix44.Heartbeat;

import java.util.function.Consumer;

/**
 * 自定义FIX应用实现，处理收到的消息，特别是Heartbeat响应
 */
public class TestApplication implements Application {
    private final Consumer<String> heartbeatHandler;
    
    public TestApplication(Consumer<String> heartbeatHandler) {
        this.heartbeatHandler = heartbeatHandler;
    }
    
    @Override
    public void onCreate(SessionID sessionId) {
        // 会话创建时调用
    }
    
    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("已登录到会话: " + sessionId);
    }
    
    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("已从会话登出: " + sessionId);
    }
    
    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // 发送管理消息时调用
    }
    
    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // 处理收到的管理消息
        try {
            // 检查是否是Heartbeat消息并且包含TestReqID
            if (message instanceof Heartbeat) {
                Heartbeat heartbeat = (Heartbeat) message;
                if (heartbeat.isSetField(TestReqID.FIELD)) {
                    String testReqID = heartbeat.get(TestReqID.FIELD).getValue();
                    // 通知处理程序收到了响应
                    heartbeatHandler.accept(testReqID);
                }
            }
        } catch (Exception e) {
            System.err.println("处理管理消息错误: " + e.getMessage());
        }
    }
    
    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        // 发送应用消息时调用
    }
    
    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // 处理收到的应用消息（在这个测试中我们主要关注Heartbeat）
    }
}
    