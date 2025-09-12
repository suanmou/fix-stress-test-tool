#!/bin/bash

# 编译项目
javac -cp "quickfixj-core-2.3.0.jar:quickfixj-msg-fix44-2.3.0.jar:commons-cli-1.4.jar" \
    com/finance/fix/tester/*.java

# 基础连接测试 - 20个会话，验证连接成功率
java -cp "quickfixj-core-2.3.0.jar:quickfixj-msg-fix44-2.3.0.jar:commons-cli-1.4.jar:." \
    com.finance.fix.tester.FixPressureTester \
    -config fixconfig.template \
    -sessions 20 \
    -messages 100 \
    -rate 10 \
    -output connection_test_report.txt

# 高并发连接测试 - 50个会话，测试连接建立性能
java -cp "quickfixj-core-2.3.0.jar:quickfixj-msg-fix44-2.3.0.jar:commons-cli-1.4.jar:." \
    com.finance.fix.tester.FixPressureTester \
    -config fixconfig.template \
    -sessions 50 \
    -duration 5 \
    -rate 15 \
    -output high_concurrent_report.txt

# 长时间稳定性测试 - 30个会话，持续30分钟
java -cp "quickfixj-core-2.3.0.jar:quickfixj-msg-fix44-2.3.0.jar:commons-cli-1.4.jar:." \
    com.finance.fix.tester.FixPressureTester \
    -config fixconfig.template \
    -sessions 30 \
    -duration 30 \
    -rate 20 \
    -output stability_test_report.txt
    