package com.financial.fix.stresstest;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReject;
import quickfix.fix44.ExecutionReport;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * FIX客户端任务，实现并发测试
 */
public class FixClientTask implements Application, Callable<Void> {
    private final String configFile;
    private final String senderCompId;
    private final int messagesToSend;
    private final List<FixTestResult> results = new ArrayList<>();
    private SessionID sessionId;
    private Initiator initiator;
    private final Random random = new Random();
    private int messagesSent = 0;
    private int messagesReceived = 0;

    public FixClientTask(String configFile, String senderCompId, int messagesToSend) {
        this.configFile = configFile;
        this.senderCompId = senderCompId;
        this.messagesToSend = messagesToSend;
    }

    @Override
    public Void call() throws Exception {
        try {
            // 加载并修改配置文件，使用独立的SenderCompID
            SessionSettings settings = new SessionSettings(new FileInputStream(configFile));
            Dictionary defaults = settings.getDefaultProperties();
            defaults.setString(SenderCompID.FIELD, senderCompId);
            settings.set(defaults);

            // 初始化FIX客户端
            initiator = new SocketInitiator(this, new FileStoreFactory(settings), 
                                           settings, new FileLogFactory(settings));
            
            initiator.start();
            
            // 等待会话建立
            while (sessionId == null) {
                Thread.sleep(100);
            }
            
            // 发送测试消息
            sendTestMessages();
            
            // 等待所有响应
            while (messagesReceived < messagesToSend) {
                Thread.sleep(100);
            }
            
            // 停止客户端
            initiator.stop();
            
        } catch (Exception e) {
            System.err.println("Client " + senderCompId + " error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * 发送测试消息
     */
    private void sendTestMessages() throws SessionNotFound, InterruptedException {
        System.out.println("Client " + senderCompId + " starting to send messages...");
        
        while (messagesSent < messagesToSend) {
            NewOrderSingle newOrder = createNewOrderSingle();
            Session.sendToTarget(newOrder, sessionId);
            
            // 记录发送时间
            results.add(new FixTestResult(
                newOrder.getClOrdID().getValue(),
                System.currentTimeMillis(),
                false,
                0,
                null
            ));
            
            messagesSent++;
            
            // 简单的流量控制，避免瞬间发送过多消息
            Thread.sleep(10);
        }
        
        System.out.println("Client " + senderCompId + " finished sending messages");
    }

    /**
     * 创建新订单消息
     */
    private NewOrderSingle createNewOrderSingle() {
        NewOrderSingle newOrder = new NewOrderSingle();
        
        // 设置必要字段
        newOrder.set(new ClOrdID("TEST-" + senderCompId + "-" + System.currentTimeMillis()));
        newOrder.set(new Symbol("TEST-" + (random.nextInt(10) + 1)));
        newOrder.set(new Side(Side.BUY));
        newOrder.set(new TransactTime());
        newOrder.set(new OrdType(OrdType.LIMIT));
        newOrder.set(new Price(random.nextDouble() * 100 + 100));
        newOrder.set(new OrderQty(random.nextDouble() * 1000 + 100));
        
        return newOrder;
    }

    /**
     * 获取测试结果
     */
    public List<FixTestResult> getResults() {
        return results;
    }

    // QuickFIX/J Application接口方法实现
    @Override
    public void onCreate(SessionID sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Client " + senderCompId + " logged on: " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Client " + senderCompId + " logged out: " + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // 可以在这里修改发送到管理员的消息
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // 处理来自管理员的消息
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        // 可以在这里修改发送到应用的消息
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // 处理来自应用的消息（响应）
        try {
            if (message instanceof ExecutionReport) {
                handleExecutionReport((ExecutionReport) message);
            } else if (message instanceof OrderCancelReject) {
                handleOrderCancelReject((OrderCancelReject) message);
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    /**
     * 处理执行报告
     */
    private void handleExecutionReport(ExecutionReport report) throws FieldNotFound {
        String clOrdId = report.getClOrdID().getValue();
        char ordStatus = report.getOrdStatus().getValue();
        
        // 查找对应的发送记录并更新
        for (FixTestResult result : results) {
            if (result.getClOrdId().equals(clOrdId) && !result.isSuccess()) {
                long responseTime = System.currentTimeMillis() - result.getSendTimeMs();
                boolean success = ordStatus == OrdStatus.NEW || ordStatus == OrdStatus.FILLED;
                String errorMessage = success ? null : "Order status: " + ordStatus;
                
                result.setSuccess(success);
                result.setResponseTimeMs(responseTime);
                result.setErrorMessage(errorMessage);
                
                messagesReceived++;
                break;
            }
        }
    }

    /**
     * 处理订单取消拒绝
     */
    private void handleOrderCancelReject(OrderCancelReject reject) throws FieldNotFound {
        String clOrdId = reject.getClOrdID().getValue();
        
        // 查找对应的发送记录并标记为失败
        for (FixTestResult result : results) {
            if (result.getClOrdId().equals(clOrdId) && !result.isSuccess()) {
                long responseTime = System.currentTimeMillis() - result.getSendTimeMs();
                result.setSuccess(false);
                result.setResponseTimeMs(responseTime);
                result.setErrorMessage("Order cancelled: " + reject.getText().getValue());
                
                messagesReceived++;
                break;
            }
        }
    }
}
    