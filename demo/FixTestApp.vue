<template>
  <div class="fix-test-app">
    <el-container>
      <el-header>
        <h1>FIX协议压力测试工具</h1>
      </el-header>
      
      <el-main>
        <el-tabs v-model="activeTab" type="card">
          <!-- 测试配置标签页 -->
          <el-tab-pane label="测试配置" name="config">
            <el-form ref="testConfigForm" :model="testConfig" :rules="rules" label-width="150px">
              <el-divider content-position="left">FIX服务器配置</el-divider>
              
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="服务器地址" prop="host">
                    <el-input v-model="testConfig.host" placeholder="请输入FIX服务器主机地址"></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="服务器端口" prop="port">
                    <el-input v-model.number="testConfig.port" placeholder="请输入FIX服务器端口"></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="FIX版本" prop="fixVersion">
                    <el-select v-model="testConfig.fixVersion" placeholder="请选择FIX版本">
                      <el-option label="FIX.4.2" value="FIX.4.2"></el-option>
                      <el-option label="FIX.4.3" value="FIX.4.3"></el-option>
                      <el-option label="FIX.4.4" value="FIX.4.4"></el-option>
                      <el-option label="FIX.5.0" value="FIX.5.0"></el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="发送方CompID" prop="senderCompId">
                    <el-input v-model="testConfig.senderCompId" placeholder="请输入发送方CompID"></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="目标方CompID" prop="targetCompId">
                    <el-input v-model="testConfig.targetCompId" placeholder="请输入目标方CompID"></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-divider content-position="left">压力测试参数</el-divider>
              
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="并发客户端数" prop="concurrentClients">
                    <el-input v-model.number="testConfig.concurrentClients" placeholder="请输入并发客户端数量"></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="每个客户端消息数" prop="messagesPerClient">
                    <el-input v-model.number="testConfig.messagesPerClient" placeholder="请输入每个客户端发送的消息数量"></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="消息发送间隔(ms)" prop="sendInterval">
                    <el-input v-model.number="testConfig.sendInterval" placeholder="请输入消息发送间隔，0表示无间隔"></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-divider content-position="left">GCP指标配置（可选）</el-divider>
              
              <el-form-item>
                <el-switch 
                  v-model="testConfig.collectGcpMetrics" 
                  active-text="启用GCP指标收集" 
                  inactive-text="禁用GCP指标收集">
                </el-switch>
              </el-form-item>
              
              <template v-if="testConfig.collectGcpMetrics">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="GCP项目ID" prop="gcpProjectId">
                      <el-input v-model="testConfig.gcpProjectId" placeholder="请输入GCP项目ID"></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="GCP实例ID" prop="gcpInstanceId">
                      <el-input v-model="testConfig.gcpInstanceId" placeholder="请输入GCP实例ID"></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="GCP区域" prop="gcpZone">
                      <el-input v-model="testConfig.gcpZone" placeholder="请输入GCP区域，如us-central1-a"></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>
              
              <el-form-item>
                <el-button type="primary" @click="startTest" :loading="isStartingTest">开始压力测试</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <!-- 测试执行标签页 -->
          <el-tab-pane label="测试执行" name="execution">
            <template v-if="currentTestId">
              <el-card>
                <div slot="header">
                  <span>当前测试 (ID: {{ currentTestId }})</span>
                </div>
                <div class="test-status">
                  <el-descriptions column="1" border>
                    <el-descriptions-item label="测试状态">{{ testStatus }}</el-descriptions-item>
                    <el-descriptions-item label="开始时间">{{ formatDate(currentTestResult.startTime) }}</el-descriptions-item>
                    <el-descriptions-item label="已持续时间">{{ formatDuration(currentTestResult.durationMs) }}</el-descriptions-item>
                    <el-descriptions-item label="已发送消息">{{ currentTestResult.totalMessages }}</el-descriptions-item>
                    <el-descriptions-item label="成功消息">{{ currentTestResult.successfulMessages }} ({{ (currentTestResult.successRate * 100).toFixed(2) }}%)</el-descriptions-item>
                    <el-descriptions-item label="失败消息">{{ currentTestResult.failedMessages }}</el-descriptions-item>
                    <el-descriptions-item label="当前吞吐量">{{ currentTestResult.throughput.toFixed(2) }} 消息/秒</el-descriptions-item>
                  </el-descriptions>
                </div>
                
                <el-progress 
                  :percentage="testProgress" 
                  stroke-width="8" 
                  style="margin-top: 20px;">
                </el-progress>
                
                <div style="margin-top: 20px; text-align: center;">
                  <el-button 
                    type="danger" 
                    @click="stopTest" 
                    :loading="isStoppingTest"
                    :disabled="testStatus !== 'RUNNING'">
                    停止测试
                  </el-button>
                </div>
              </el-card>
            </template>
            
            <template v-else>
              <el-empty description="没有正在执行的测试，请在测试配置页开始新的测试"></el-empty>
            </template>
          </el-tab-pane>
          
          <!-- 测试结果标签页 -->
          <el-tab-pane label="测试结果" name="results">
            <el-card>
              <div slot="header">
                <span>测试结果列表</span>
              </div>
              
              <el-table :data="testHistory" style="width: 100%">
                <el-table-column prop="testId" label="测试ID" width="200"></el-table-column>
                <el-table-column prop="startTime" label="开始时间" :formatter="formatDate"></el-table-column>
                <el-table-column prop="durationMs" label="持续时间" :formatter="formatDuration"></el-table-column>
                <el-table-column prop="totalMessages" label="总消息数"></el-table-column>
                <el-table-column prop="successRate" label="成功率" :formatter="formatPercentage"></el-table-column>
                <el-table-column prop="throughput" label="吞吐量" :formatter="formatThroughput"></el-table-column>
                <el-table-column label="操作">
                  <template slot-scope="scope">
                    <el-button 
                      type="text" 
                      @click="viewTestResult(scope.row.testId)">
                      查看详情
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
            
            <!-- 测试结果详情 -->
            <el-dialog 
              title="测试结果详情" 
              :visible.sync="resultDialogVisible" 
              width="90%"
              :close-on-click-modal="false">
              
              <template v-if="selectedTestResult">
                <el-tabs type="border-card">
                  <el-tab-pane label="概览">
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-card>
                          <div slot="header">测试配置</div>
                          <el-descriptions column="1" border>
                            <el-descriptions-item label="测试ID">{{ selectedTestResult.testId }}</el-descriptions-item>
                            <el-descriptions-item label="服务器地址">{{ selectedTestResult.config.host }}:{{ selectedTestResult.config.port }}</el-descriptions-item>
                            <el-descriptions-item label="FIX版本">{{ selectedTestResult.config.fixVersion }}</el-descriptions-item>
                            <el-descriptions-item label="发送方/目标方">{{ selectedTestResult.config.senderCompId }} / {{ selectedTestResult.config.targetCompId }}</el-descriptions-item>
                            <el-descriptions-item label="并发客户端数">{{ selectedTestResult.config.concurrentClients }}</el-descriptions-item>
                            <el-descriptions-item label="总消息数">{{ selectedTestResult.totalMessages }}</el-descriptions-item>
                          </el-descriptions>
                        </el-card>
                      </el-col>
                      
                      <el-col :span="12">
                        <el-card>
                          <div slot="header">测试统计</div>
                          <el-descriptions column="1" border>
                            <el-descriptions-item label="开始时间">{{ formatDate(selectedTestResult.startTime) }}</el-descriptions-item>
                            <el-descriptions-item label="结束时间">{{ formatDate(selectedTestResult.endTime) }}</el-descriptions-item>
                            <el-descriptions-item label="持续时间">{{ formatDuration(selectedTestResult.durationMs) }}</el-descriptions-item>
                            <el-descriptions-item label="成功消息数">{{ selectedTestResult.successfulMessages }}</el-descriptions-item>
                            <el-descriptions-item label="失败消息数">{{ selectedTestResult.failedMessages }}</el-descriptions-item>
                            <el-descriptions-item label="成功率">{{ (selectedTestResult.successRate * 100).toFixed(2) }}%</el-descriptions-item>
                            <el-descriptions-item label="平均吞吐量">{{ selectedTestResult.throughput.toFixed(2) }} 消息/秒</el-descriptions-item>
                          </el-descriptions>
                        </el-card>
                      </el-col>
                    </el-row>
                  </el-tab-pane>
                  
                  <el-tab-pane label="响应时间分析">
                    <el-card>
                      <div slot="header">响应时间统计 (毫秒)</div>
                      <el-row :gutter="20">
                        <el-col :span="6">
                          <div class="stat-card">
                            <div class="stat-label">最小响应时间</div>
                            <div class="stat-value">{{ selectedTestResult.minResponseTime }} ms</div>
                          </div>
                        </el-col>
                        <el-col :span="6">
                          <div class="stat-card">
                            <div class="stat-label">最大响应时间</div>
                            <div class="stat-value">{{ selectedTestResult.maxResponseTime }} ms</div>
                          </div>
                        </el-col>
                        <el-col :span="6">
                          <div class="stat-card">
                            <div class="stat-label">平均响应时间</div>
                            <div class="stat-value">{{ selectedTestResult.avgResponseTime.toFixed(2) }} ms</div>
                          </div>
                        </el-col>
                        <el-col :span="6">
                          <div class="stat-card">
                            <div class="stat-label">P99响应时间</div>
                            <div class="stat-value">{{ selectedTestResult.percentileResponseTimes.P99 }} ms</div>
                          </div>
                        </el-col>
                      </el-row>
                      
                      <div style="margin-top: 20px; height: 300px;">
                        <canvas id="responseTimeChart"></canvas>
                      </div>
                    </el-card>
                  </el-tab-pane>
                  
                  <el-tab-pane label="错误分析">
                    <el-card>
                      <div slot="header">错误分布</div>
                      <div v-if="Object.keys(selectedTestResult.errorDistribution).length > 0">
                        <div style="height: 300px;">
                          <canvas id="errorDistributionChart"></canvas>
                        </div>
                        
                        <el-table 
                          :data="errorDistributionList" 
                          style="width: 100%; margin-top: 20px;">
                          <el-table-column prop="error" label="错误类型"></el-table-column>
                          <el-table-column prop="count" label="数量"></el-table-column>
                          <el-table-column prop="percentage" label="占比" :formatter="formatPercentage"></el-table-column>
                        </el-table>
                      </div>
                      <div v-else>
                        <el-empty description="测试过程中没有错误发生"></el-empty>
                      </div>
                    </el-card>
                  </el-tab-pane>
                  
                  <el-tab-pane label="GCP指标" v-if="selectedTestResult.config.collectGcpMetrics">
                    <el-card>
                      <div slot="header">GCP性能指标</div>
                      <template v-if="selectedTestResult.gcpMetrics">
                        <el-row :gutter="20">
                          <el-col :span="12">
                            <div style="height: 300px;">
                              <canvas id="cpuUsageChart"></canvas>
                            </div>
                          </el-col>
                          <el-col :span="12">
                            <div style="height: 300px;">
                              <canvas id="networkThroughputChart"></canvas>
                            </div>
                          </el-col>
                        </el-row>
                        
                        <el-row :gutter="20" style="margin-top: 20px;">
                          <el-col :span="12">
                            <div style="height: 300px;">
                              <canvas id="networkLatencyChart"></canvas>
                            </div>
                          </el-col>
                          <el-col :span="12">
                            <div style="height: 300px;">
                              <canvas id="diskIopsChart"></canvas>
                            </div>
                          </el-col>
                        </el-row>
                      </template>
                      <template v-else>
                        <el-empty description="未收集到GCP指标数据"></el-empty>
                      </template>
                    </el-card>
                  </el-tab-pane>
                </el-tabs>
              </template>
              
              <div slot="footer" style="text-align: right;">
                <el-button @click="resultDialogVisible = false">关闭</el-button>
              </div>
            </el-dialog>
          </el-tab-pane>
        </el-tabs>
      </el-main>
      
      <el-footer>
        FIX协议压力测试工具 © 2023
      </el-footer>
    </el-container>
  </div>
</template>

<script>
import axios from 'axios';
import Chart from 'chart.js';

export default {
  name: 'FixTestApp',
  data() {
    return {
      activeTab: 'config',
      testConfig: {
        host: 'localhost',
        port: 1234,
        fixVersion: 'FIX.4.4',
        senderCompId: 'TEST-SENDER',
        targetCompId: 'TEST-TARGET',
        concurrentClients: 1,
        messagesPerClient: 100,
        sendInterval: 0,
        collectGcpMetrics: false,
        gcpProjectId: '',
        gcpInstanceId: '',
        gcpZone: ''
      },
      rules: {
        host: [
          { required: true, message: '请输入服务器地址', trigger: 'blur' }
        ],
        port: [
          { required: true, message: '请输入服务器端口', trigger: 'blur' },
          { type: 'number', message: '端口必须是数字', trigger: 'blur' },
          { min: 1, max: 65535, message: '端口必须在1-65535之间', trigger: 'blur' }
        ],
        senderCompId: [
          { required: true, message: '请输入发送方CompID', trigger: 'blur' }
        ],
        targetCompId: [
          { required: true, message: '请输入目标方CompID', trigger: 'blur' }
        ],
        concurrentClients: [
          { type: 'number', message: '必须是数字', trigger: 'blur' },
          { min: 1, message: '至少为1', trigger: 'blur' }
        ],
        messagesPerClient: [
          { type: 'number', message: '必须是数字', trigger: 'blur' },
          { min: 1, message: '至少为1', trigger: 'blur' }
        ],
        sendInterval: [
          { type: 'number', message: '必须是数字', trigger: 'blur' },
          { min: 0, message: '不能为负数', trigger: 'blur' }
        ],
        gcpProjectId: [
          { required: true, message: '请输入GCP项目ID', trigger: 'blur' }
        ],
        gcpInstanceId: [
          { required: true, message: '请输入GCP实例ID', trigger: 'blur' }
        ],
        gcpZone: [
          { required: true, message: '请输入GCP区域', trigger: 'blur' }
        ]
      },
      currentTestId: '',
      testStatus: 'NOT_STARTED',
      currentTestResult: {
        totalMessages: 0,
        successfulMessages: 0,
        failedMessages: 0,
        successRate: 0,
        throughput: 0,
        startTime: null,
        durationMs: 0
      },
      testProgress: 0,
      isStartingTest: false,
      isStoppingTest: false,
      testHistory: [],
      selectedTestResult: null,
      resultDialogVisible: false,
      errorDistributionList: [],
      // 图表实例
      charts: {}
    };
  },
  mounted() {
    // 加载历史测试记录
    this.loadTestHistory();
    
    // 检查是否有正在运行的测试
    this.checkRunningTest();
  },
  methods: {
    // 开始压力测试
    startTest() {
      this.$refs.testConfigForm.validate(async (valid) => {
        if (valid) {
          this.isStartingTest = true;
          try {
            const response = await axios.post('/api/fix-test/start', this.testConfig);
            this.currentTestId = response.data;
            this.testStatus = 'RUNNING';
            this.activeTab = 'execution';
            
            // 开始轮询测试状态
            this.startPollingTestStatus();
            
            this.$message.success('压力测试已启动');
          } catch (error) {
            this.$message.error('启动压力测试失败: ' + (error.response?.data || error.message));
          } finally {
            this.isStartingTest = false;
          }
        }
      });
    },
    
    // 停止压力测试
    stopTest() {
      this.$confirm('确定要停止当前测试吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        this.isStoppingTest = true;
        try {
          await axios.post(`/api/fix-test/stop/${this.currentTestId}`);
          this.$message.success('已发送停止测试请求');
        } catch (error) {
          this.$message.error('停止测试失败: ' + (error.response?.data || error.message));
        } finally {
          this.isStoppingTest = false;
        }
      }).catch(() => {
        // 取消操作
      });
    },
    
    // 轮询测试状态
    startPollingTestStatus() {
      const poll = async () => {
        if (!this.currentTestId || this.testStatus === 'COMPLETED' || this.testStatus === 'STOPPED') {
          return;
        }
        
        try {
          // 获取测试状态
          const statusResponse = await axios.get(`/api/fix-test/status/${this.currentTestId}`);
          this.testStatus = statusResponse.data;
          
          // 获取测试结果
          const resultResponse = await axios.get(`/api/fix-test/result/${this.currentTestId}`);
          this.currentTestResult = resultResponse.data;
          
          // 计算进度
          const totalExpected = this.testConfig.concurrentClients * this.testConfig.messagesPerClient;
          this.testProgress = totalExpected > 0 ? (this.currentTestResult.totalMessages / totalExpected) * 100 : 0;
          
          // 如果测试已完成，更新历史记录
          if (this.testStatus === 'COMPLETED' || this.testStatus === 'STOPPED') {
            this.loadTestHistory();
          } else {
            // 继续轮询
            setTimeout(poll, 2000);
          }
        } catch (error) {
          console.error('获取测试状态失败', error);
          setTimeout(poll, 5000);
        }
      };
      
      // 立即开始第一次轮询
      poll();
    },
    
    // 加载测试历史
    async loadTestHistory() {
      try {
        const response = await axios.get('/api/fix-test/history');
        this.testHistory = response.data;
      } catch (error) {
        this.$message.error('加载测试历史失败: ' + (error.response?.data || error.message));
      }
    },
    
    // 查看测试结果详情
    async viewTestResult(testId) {
      try {
        const response = await axios.get(`/api/fix-test/result/${testId}`);
        this.selectedTestResult = response.data;
        
        // 准备错误分布列表
        this.errorDistributionList = Object.entries(this.selectedTestResult.errorDistribution).map(([error, count]) => ({
          error,
          count,
          percentage: count / this.selectedTestResult.totalMessages
        }));
        
        this.resultDialogVisible = true;
        
        // 延迟初始化图表，确保DOM已更新
        setTimeout(() => {
          this.initCharts();
        }, 100);
      } catch (error) {
        this.$message.error('获取测试结果失败: ' + (error.response?.data || error.message));
      }
    },
    
    // 初始化图表
    initCharts() {
      // 销毁已存在的图表
      Object.values(this.charts).forEach(chart => chart.destroy());
      
      // 响应时间图表
      this.charts.responseTimeChart = new Chart(
        document.getElementById('responseTimeChart'),
        {
          type: 'bar',
          data: {
            labels: Object.keys(this.selectedTestResult.percentileResponseTimes),
            datasets: [{
              label: '响应时间 (ms)',
              data: Object.values(this.selectedTestResult.percentileResponseTimes),
              backgroundColor: 'rgba(54, 162, 235, 0.5)',
              borderColor: 'rgba(54, 162, 235, 1)',
              borderWidth: 1
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
              y: {
                beginAtZero: true,
                title: {
                  display: true,
                  text: '响应时间 (ms)'
                }
              }
            },
            title: {
              display: true,
              text: '响应时间百分位数分布'
            }
          }
        }
      );
      
      // 错误分布图表
      if (Object.keys(this.selectedTestResult.errorDistribution).length > 0) {
        this.charts.errorDistributionChart = new Chart(
          document.getElementById('errorDistributionChart'),
          {
            type: 'pie',
            data: {
              labels: Object.keys(this.selectedTestResult.errorDistribution),
              datasets: [{
                data: Object.values(this.selectedTestResult.errorDistribution),
                backgroundColor: [
                  'rgba(255, 99, 132, 0.5)',
                  'rgba(54, 162, 235, 0.5)',
                  'rgba(255, 206, 86, 0.5)',
                  'rgba(75, 192, 192, 0.5)',
                  'rgba(153, 102, 255, 0.5)',
                  'rgba(255, 159, 64, 0.5)'
                ],
                borderColor: [
                  'rgba(255, 99, 132, 1)',
                  'rgba(54, 162, 235, 1)',
                  'rgba(255, 206, 86, 1)',
                  'rgba(75, 192, 192, 1)',
                  'rgba(153, 102, 255, 1)',
                  'rgba(255, 159, 64, 1)'
                ],
                borderWidth: 1
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              title: {
                display: true,
                text: '错误分布'
              }
            }
          }
        );
      }
      
      // GCP指标图表
      if (this.selectedTestResult.config.collectGcpMetrics && this.selectedTestResult.gcpMetrics) {
        const gcpMetrics = this.selectedTestResult.gcpMetrics;
        
        // CPU使用率图表
        this.charts.cpuUsageChart = new Chart(
          document.getElementById('cpuUsageChart'),
          {
            type: 'line',
            data: {
              labels: gcpMetrics.timestamps,
              datasets: [{
                label: 'CPU使用率 (%)',
                data: gcpMetrics.cpuUsage,
                borderColor: 'rgba(255, 99, 132, 1)',
                backgroundColor: 'rgba(255, 99, 132, 0.1)',
                fill: true,
                tension: 0.1
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              scales: {
                y: {
                  beginAtZero: true,
                  max: 100,
                  title: {
                    display: true,
                    text: 'CPU使用率 (%)'
                  }
                }
              },
              title: {
                display: true,
                text: 'CPU使用率趋势'
              }
            }
          }
        );
        
        // 网络吞吐量图表
        this.charts.networkThroughputChart = new Chart(
          document.getElementById('networkThroughputChart'),
          {
            type: 'line',
            data: {
              labels: gcpMetrics.timestamps,
              datasets: [{
                label: '接收 (MB/s)',
                data: gcpMetrics.networkReceive,
                borderColor: 'rgba(54, 162, 235, 1)',
                backgroundColor: 'rgba(54, 162, 235, 0.1)',
                fill: true,
                tension: 0.1
              }, {
                label: '发送 (MB/s)',
                data: gcpMetrics.networkSend,
                borderColor: 'rgba(75, 192, 192, 1)',
                backgroundColor: 'rgba(75, 192, 192, 0.1)',
                fill: true,
                tension: 0.1
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              scales: {
                y: {
                  beginAtZero: true,
                  title: {
                    display: true,
                    text: '吞吐量 (MB/s)'
                  }
                }
              },
              title: {
                display: true,
                text: '网络吞吐量趋势'
              }
            }
          }
        );
        
        // 网络延迟图表
        this.charts.networkLatencyChart = new Chart(
          document.getElementById('networkLatencyChart'),
          {
            type: 'line',
            data: {
              labels: gcpMetrics.timestamps,
              datasets: [{
                label: '网络延迟 (ms)',
                data: gcpMetrics.networkLatency,
                borderColor: 'rgba(255, 206, 86, 1)',
                backgroundColor: 'rgba(255, 206, 86, 0.1)',
                fill: true,
                tension: 0.1
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              scales: {
                y: {
                  beginAtZero: true,
                  title: {
                    display: true,
                    text: '延迟 (ms)'
                  }
                }
              },
              title: {
                display: true,
                text: '网络延迟趋势'
              }
            }
          }
        );
        
        // 磁盘IOPS图表
        this.charts.diskIopsChart = new Chart(
          document.getElementById('diskIopsChart'),
          {
            type: 'line',
            data: {
              labels: gcpMetrics.timestamps,
              datasets: [{
                label: '读IOPS',
                data: gcpMetrics.diskReadIops,
                borderColor: 'rgba(153, 102, 255, 1)',
                backgroundColor: 'rgba(153, 102, 255, 0.1)',
                fill: true,
                tension: 0.1
              }, {
                label: '写IOPS',
                data: gcpMetrics.diskWriteIops,
                borderColor: 'rgba(255, 159, 64, 1)',
                backgroundColor: 'rgba(255, 159, 64, 0.1)',
                fill: true,
                tension: 0.1
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              scales: {
                y: {
                  beginAtZero: true,
                  title: {
                    display: true,
                    text: 'IOPS'
                  }
                }
              },
              title: {
                display: true,
                text: '磁盘IOPS趋势'
              }
            }
          }
        );
      }
    },
    
    // 检查是否有正在运行的测试
    async checkRunningTest() {
      // 实际实现中，这里应该查询后端获取当前是否有运行中的测试
      // 这里简化处理
    },
    
    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return '-';
      const date = new Date(dateStr);
      return date.toLocaleString();
    },
    
    // 格式化持续时间
    formatDuration(ms) {
      if (!ms || ms <= 0) return '-';
      
      const seconds = Math.floor(ms / 1000);
      const minutes = Math.floor(seconds / 60);
      const hours = Math.floor(minutes / 60);
      
      const remainingSeconds = seconds % 60;
      const remainingMinutes = minutes % 60;
      
      let result = '';
      if (hours > 0) result += `${hours}小时 `;
      if (remainingMinutes > 0) result += `${remainingMinutes}分钟 `;
      result += `${remainingSeconds}秒`;
      
      return result;
    },
    
    // 格式化百分比
    formatPercentage(value) {
      return (value * 100).toFixed(2) + '%';
    },
    
    // 格式化吞吐量
    formatThroughput(value) {
      return value.toFixed(2) + ' 消息/秒';
    }
  },
  beforeDestroy() {
    // 销毁图表
    Object.values(this.charts).forEach(chart => chart.destroy());
  }
};
</script>

<style scoped>
.fix-test-app {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.el-header {
  background-color: #1E88E5;
  color: white;
  text-align: center;
  line-height: 60px;
}

.el-footer {
  background-color: #f5f7fa;
  color: #475669;
  text-align: center;
  line-height: 60px;
}

.el-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.stat-card {
  background-color: #f5f7fa;
  border-radius: 4px;
  padding: 16px;
  text-align: center;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stat-label {
  color: #475669;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-value {
  color: #1E88E5;
  font-size: 24px;
  font-weight: bold;
}

.test-status {
  margin-bottom: 20px;
}
</style>
    