import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class FixClientTask implements Callable<Void>, Application {
    private final String configPath;
    private final String senderCompId;
    private final String targetCompId;
    private final int messageCount;
    private final List<FixTestResult> results;
    private final Map<String, Instant> sentMessages = new ConcurrentHashMap<>();
    
    public FixClientTask(String configPath, String senderCompId, String targetCompId,
                        int messageCount, List<FixTestResult> results) {
        this.configPath = configPath;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.messageCount = messageCount;
        this.results = results;
    }

    @Override
    public Void call() throws Exception {
        // 加载并修改配置
        SessionSettings settings = new SessionSettings(configPath);
        
        // 为每个客户端设置唯一的SenderCompID
        Dictionary sessionDict = settings.get(sessionKey());
        sessionDict.setString(SenderCompID.FIELD, senderCompId);
        sessionDict.setString(TargetCompID.FIELD, targetCompId);
        
        // 创建FIX会话
        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        Initiator initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
        
        try {
            initiator.start();
            System.out.println("客户端 " + senderCompId + " 已启动，准备发送 " + messageCount + " 条消息");
            
            // 等待会话连接成功
            SessionID sessionId = initiator.getSessions().get(0);
            int maxRetries = 30;
            int retryCount = 0;
            
            while (!Session.lookupSession(sessionId).isLoggedOn() && retryCount < maxRetries) {
                Thread.sleep(1000);
                retryCount++;
            }
            
            if (!Session.lookupSession(sessionId).isLoggedOn()) {
                throw new Exception("客户端 " + senderCompId + " 无法连接到服务器");
            }
            
            // 发送消息
            sendMessages(sessionId);
            
        } finally {
            initiator.stop();
            System.out.println("客户端 " + senderCompId + " 已停止");
        }
        
        return null;
    }
    
    private void sendMessages(SessionID sessionId) throws SessionNotFound, InterruptedException {
        for (int i = 0; i < messageCount; i++) {
            // 创建新订单消息
            NewOrderSingle newOrder = createNewOrderSingle(i);
            
            // 记录发送时间
            String clOrdId = newOrder.getClOrdID().getValue();
            Instant sendTime = Instant.now();
            sentMessages.put(clOrdId, sendTime);
            
            // 发送消息
            boolean sent = Session.sendToTarget(newOrder, sessionId);
            
            if (!sent) {
                sentMessages.remove(clOrdId);
                results.add(new FixTestResult(0, false, "消息发送失败: " + clOrdId));
            }
            
            // 简单限流，避免消息发送过快导致客户端处理不过来
            Thread.sleep(10);
        }
        
        // 等待所有响应返回或超时
        long timeoutMs = 30000; // 30秒超时
        long waitIntervalMs = 100;
        long totalWaitMs = 0;
        
        while (!sentMessages.isEmpty() && totalWaitMs < timeoutMs) {
            Thread.sleep(waitIntervalMs);
            totalWaitMs += waitIntervalMs;
        }
        
        // 记录超时的消息
        for (Map.Entry<String, Instant> entry : sentMessages.entrySet()) {
            results.add(new FixTestResult(
                Duration.between(entry.getValue(), Instant.now()).toMillis(),
                false,
                "消息超时未收到响应: " + entry.getKey()
            ));
        }
    }
    
    // 创建新订单消息
    private NewOrderSingle createNewOrderSingle(int sequence) {
        NewOrderSingle newOrder = new NewOrderSingle();
        
        // 设置必要的字段
        newOrder.set(new ClOrdID(UUID.randomUUID().toString()));
        newOrder.set(new Symbol("TEST-" + (sequence % 100))); // 随机100个不同的股票代码
        newOrder.set(new Side(Side.BUY));
        newOrder.set(new TransactTime());
        newOrder.set(new OrdType(OrdType.MARKET));
        newOrder.set(new OrderQty(100 + (sequence % 900))); // 100-1000之间的数量
        
        return newOrder;
    }
    
    private SessionID sessionKey() {
        return new SessionID(BeginString.FIX44, senderCompId, targetCompId);
    }

    // QuickFIX/J Application接口实现
    @Override
    public void onCreate(SessionID sessionId) {}

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("客户端 " + senderCompId + " 登录成功");
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("客户端 " + senderCompId + " 登出");
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {}

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {}

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {}

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        try {
            // 处理执行报告
            if (message instanceof ExecutionReport) {
                ExecutionReport report = (ExecutionReport) message;
                ClOrdID clOrdID = new ClOrdID();
                report.get(clOrdID);
                
                // 查找对应的发送时间
                Instant sendTime = sentMessages.remove(clOrdID.getValue());
                if (sendTime != null) {
                    long responseTimeMs = Duration.between(sendTime, Instant.now()).toMillis();
                    results.add(new FixTestResult(responseTimeMs, true, null));
                }
            }
        } catch (Exception e) {
            System.err.println("处理消息时出错: " + e.getMessage());
        }
    }
}
    