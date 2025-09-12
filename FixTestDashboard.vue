<template>
  <div class="fix-test-dashboard">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="logo">
        <i class="el-icon-connection"></i>
        <span>FIX压力测试平台</span>
      </div>
      <el-menu 
        :default-active="activeMenu" 
        mode="horizontal" 
        background-color="#1f2d3d" 
        text-color="#fff" 
        active-text-color="#409EFF"
        class="main-menu"
      >
        <el-menu-item index="dashboard">仪表盘</el-menu-item>
        <el-menu-item index="history">历史记录</el-menu-item>
        <el-menu-item index="settings">系统设置</el-menu-item>
      </el-menu>
      <div class="user-info">
        <el-avatar icon="el-icon-user"></el-avatar>
        <span class="username">测试管理员</span>
      </div>
    </el-header>

    <!-- 主要内容区域 -->
    <el-container class="main-container">
      <el-aside width="250px" class="sidebar">
        <el-card>
          <div slot="header">
            <span>测试导航</span>
          </div>
          <el-menu
            :default-active="activeSubMenu"
            class="sidebar-menu"
            @select="handleSubMenuSelect"
          >
            <el-menu-item index="new-test">
              <i class="el-icon-plus"></i>
              <span>新建测试</span>
            </el-menu-item>
            <el-menu-item index="current-test">
              <i class="el-icon-loading"></i>
              <span>当前测试</span>
            </el-menu-item>
            <el-menu-item index="test-results">
              <i class="el-icon-document"></i>
              <span>测试结果</span>
            </el-menu-item>
            <el-menu-item index="system-monitor">
              <i class="el-icon-cpu"></i>
              <span>系统监控</span>
            </el-menu-item>
          </el-menu>
        </el-card>

        <el-card class="system-status-card" v-if="systemStatus">
          <div slot="header">
            <span>系统状态</span>
          </div>
          <div class="status-item">
            <span>引擎连接</span>
            <el-badge :value="systemStatus.engineConnected ? '正常' : '断开'" :type="systemStatus.engineConnected ? 'success' : 'danger'"></el-badge>
          </div>
          <div class="status-item">
            <span>当前测试数</span>
            <el-badge :value="systemStatus.activeTests" type="info"></el-badge>
          </div>
          <div class="status-item">
            <span>系统负载</span>
            <el-progress :percentage="systemStatus.systemLoad" stroke-width="6" :stroke-color="getLoadColor(systemStatus.systemLoad)"></el-progress>
          </div>
        </el-card>
      </el-aside>

      <el-main class="content-area">
        <!-- 新建测试表单 -->
        <el-card v-if="activeSubMenu === 'new-test'" class="content-card">
          <div slot="header">
            <span>配置压力测试参数</span>
          </div>
          <el-form :model="testParams" :rules="testRules" ref="testForm" label-width="120px" class="test-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="会话数量" prop="sessions">
                  <el-input-number 
                    v-model="testParams.sessions" 
                    :min="1" 
                    :max="100" 
                    :step="1" 
                    placeholder="输入并发会话数量"
                    controls-position="right"
                  ></el-input-number>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="消息速率(TPS)" prop="rate">
                  <el-input-number 
                    v-model="testParams.rate" 
                    :min="1" 
                    :max="1000" 
                    :step="1" 
                    placeholder="每秒发送消息数"
                    controls-position="right"
                  ></el-input-number>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="测试模式">
                  <el-radio-group v-model="testMode" @change="handleTestModeChange">
                    <el-radio label="messageCount">按消息总数</el-radio>
                    <el-radio label="duration">按持续时间</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="消息总数" prop="messages" v-if="testMode === 'messageCount'">
                  <el-input-number 
                    v-model="testParams.messages" 
                    :min="100" 
                    :max="1000000" 
                    :step="100" 
                    placeholder="总消息数量"
                    controls-position="right"
                  ></el-input-number>
                </el-form-item>
                <el-form-item label="持续时间(分钟)" prop="duration" v-if="testMode === 'duration'">
                  <el-input-number 
                    v-model="testParams.duration" 
                    :min="1" 
                    :max="120" 
                    :step="1" 
                    placeholder="测试持续时间"
                    controls-position="right"
                  ></el-input-number>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="超时时间(秒)" prop="timeout">
                  <el-input-number 
                    v-model="testParams.timeout" 
                    :min="1" 
                    :max="30" 
                    :step="1" 
                    placeholder="响应超时时间"
                    controls-position="right"
                  ></el-input-number>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="配置文件路径" prop="configPath">
                  <el-input 
                    v-model="testParams.configPath" 
                    placeholder="FIX配置文件路径"
                  ></el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="高级选项">
                  <el-checkbox v-model="testParams.detailedLog">启用详细日志</el-checkbox>
                  <el-checkbox v-model="testParams.recordSystemMetrics">记录系统指标</el-checkbox>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item class="form-actions">
              <el-button type="primary" @click="startTest" :loading="isStartingTest">启动测试</el-button>
              <el-button @click="resetForm">重置</el-button>
              <el-button type="text" @click="loadTemplate">加载模板</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 当前测试监控 -->
        <el-card v-if="activeSubMenu === 'current-test'" class="content-card">
          <div slot="header">
            <span>当前测试监控</span>
            <el-button 
              type="danger" 
              size="mini" 
              style="float: right" 
              @click="stopTest" 
              v-if="currentTest && currentTest.status === 'RUNNING'"
            >
              终止测试
            </el-button>
          </div>

          <div v-if="!currentTest" class="empty-state">
            <i class="el-icon-information"></i>
            <p>当前没有正在运行的测试</p>
            <el-button type="primary" @click="switchToNewTest">新建测试</el-button>
          </div>

          <div v-if="currentTest" class="test-monitoring">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-card class="stat-card">
                  <div class="stat-title">测试ID</div>
                  <div class="stat-value">{{ currentTest.taskId }}</div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card class="stat-card">
                  <div class="stat-title">状态</div>
                  <div class="stat-value">
                    <el-tag :type="getStatusTagType(currentTest.status)">{{ currentTest.status }}</el-tag>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card class="stat-card">
                  <div class="stat-title">开始时间</div>
                  <div class="stat-value">{{ formatTime(currentTest.startTime) }}</div>
                </el-card>
              </el-col>
            </el-row>

            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="12">
                <el-card>
                  <div slot="header">测试进度</div>
                  <div class="progress-container">
                    <el-progress 
                      :percentage="currentTest.progress || 0" 
                      stroke-width="8"
                      :status="currentTest.progress === 100 ? 'success' : ''"
                    ></el-progress>
                    <div class="progress-stats">
                      <div>已发送: {{ currentTest.stats?.totalMessagesSent || 0 }} 条</div>
                      <div>已接收: {{ currentTest.stats?.totalResponsesReceived || 0 }} 条</div>
                      <div>超时: {{ currentTest.stats?.timeoutCount || 0 }} 条</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card>
                  <div slot="header">实时性能</div>
                  <div class="performance-stats">
                    <div class="performance-item">
                      <div class="performance-label">当前TPS</div>
                      <div class="performance-value">{{ currentTest.stats?.actualRate || 0 }}</div>
                    </div>
                    <div class="performance-item">
                      <div class="performance-label">平均响应时间</div>
                      <div class="performance-value">{{ currentTest.stats?.averageResponseTime || 0 }} ms</div>
                    </div>
                    <div class="performance-item">
                      <div class="performance-label">连接成功率</div>
                      <div class="performance-value">{{ currentTest.stats?.connectionSuccessRate || 0 }}%</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="24">
                <el-card>
                  <div slot="header">响应时间趋势</div>
                  <div class="chart-container">
                    <canvas id="responseTimeChart"></canvas>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="24">
                <el-card>
                  <div slot="header">实时日志</div>
                  <div class="log-container">
                    <el-scrollbar style="height: 300px;">
                      <div v-for="(log, index) in testLogs" :key="index" class="log-entry" :class="{'error-log': log.level === 'ERROR'}">
                        <span class="log-time">{{ formatTime(log.timestamp) }}</span>
                        <span class="log-level" :class="'level-' + log.level.toLowerCase()">{{ log.level }}</span>
                        <span class="log-message">{{ log.message }}</span>
                      </div>
                    </el-scrollbar>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-card>

        <!-- 测试结果展示 -->
        <el-card v-if="activeSubMenu === 'test-results'" class="content-card">
          <div slot="header">
            <span>测试结果历史</span>
            <el-input 
              placeholder="搜索测试ID" 
              v-model="searchTestId" 
              class="search-input"
              clearable
            ></el-input>
          </div>

          <el-table 
            :data="filteredTestReports" 
            border 
            style="width: 100%; margin-top: 10px;"
            v-loading="loadingTestReports"
          >
            <el-table-column prop="taskId" label="测试ID" width="200"></el-table-column>
            <el-table-column prop="status" label="状态">
              <template slot-scope="scope">
                <el-tag :type="getStatusTagType(scope.row.status)">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" width="180">
              <template slot-scope="scope">{{ formatTime(scope.row.startTime) }}</template>
            </el-table-column>
            <el-table-column prop="endTime" label="结束时间" width="180">
              <template slot-scope="scope">{{ formatTime(scope.row.endTime) }}</template>
            </el-table-column>
            <el-table-column prop="totalSessions" label="会话数"></el-table-column>
            <el-table-column prop="actualRate" label="平均TPS"></el-table-column>
            <el-table-column prop="averageResponseTime" label="平均响应时间(ms)"></el-table-column>
            <el-table-column prop="connectionSuccessRate" label="连接成功率(%)"></el-table-column>
            <el-table-column label="操作" width="160">
              <template slot-scope="scope">
                <el-button 
                  type="text" 
                  size="small" 
                  @click="viewTestReport(scope.row.taskId)"
                >
                  查看详情
                </el-button>
                <el-button 
                  type="text" 
                  size="small" 
                  @click="downloadReport(scope.row.taskId)"
                >
                  下载
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="currentPage"
            :page-sizes="[10, 20, 50]"
            :page-size="pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="testReports.length"
            style="margin-top: 15px; text-align: right;"
          ></el-pagination>
        </el-card>

        <!-- 系统监控 -->
        <el-card v-if="activeSubMenu === 'system-monitor'" class="content-card">
          <div slot="header">
            <span>系统资源监控</span>
          </div>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card>
                <div slot="header">CPU使用率</div>
                <div class="chart-container">
                  <canvas id="cpuChart"></canvas>
                </div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card>
                <div slot="header">内存使用率</div>
                <div class="chart-container">
                  <canvas id="memoryChart"></canvas>
                </div>
              </el-card>
            </el-col>
          </el-row>
          
          <el-row :gutter="20" style="margin-top: 20px;">
            <el-col :span="12">
              <el-card>
                <div slot="header">网络吞吐量</div>
                <div class="chart-container">
                  <canvas id="networkChart"></canvas>
                </div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card>
                <div slot="header">会话连接状态</div>
                <div class="chart-container">
                  <canvas id="sessionsChart"></canvas>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-main>
    </el-container>

    <!-- 测试详情对话框 -->
    <el-dialog
      title="测试详情报告"
      :visible.sync="reportDialogVisible"
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="currentReport" class="report-details">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="report-section">
              <h3>基本信息</h3>
              <el-descriptions column="1" border>
                <el-descriptions-item label="测试ID">{{ currentReport.taskId }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ currentReport.status }}</el-descriptions-item>
                <el-descriptions-item label="开始时间">{{ formatTime(currentReport.startTime) }}</el-descriptions-item>
                <el-descriptions-item label="结束时间">{{ formatTime(currentReport.endTime) }}</el-descriptions-item>
                <el-descriptions-item label="持续时间">{{ formatDuration(currentReport.startTime, currentReport.endTime) }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="report-section">
              <h3>测试参数</h3>
              <el-descriptions column="1" border>
                <el-descriptions-item label="会话数量">{{ currentReport.totalSessions }}</el-descriptions-item>
                <el-descriptions-item label="目标TPS">{{ currentReport.targetRate }}</el-descriptions-item>
                <el-descriptions-item label="实际平均TPS">{{ currentReport.actualRate }}</el-descriptions-item>
                <el-descriptions-item label="超时设置">{{ currentReport.timeout }}秒</el-descriptions-item>
                <el-descriptions-item label="总消息数">{{ currentReport.totalMessagesSent }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <div class="report-section">
              <h3>性能指标</h3>
              <el-descriptions column="2" border>
                <el-descriptions-item label="平均响应时间">{{ currentReport.averageResponseTime }}ms</el-descriptions-item>
                <el-descriptions-item label="95%响应时间">{{ currentReport.p95ResponseTime }}ms</el-descriptions-item>
                <el-descriptions-item label="99%响应时间">{{ currentReport.p99ResponseTime }}ms</el-descriptions-item>
                <el-descriptions-item label="响应成功率">{{ calculateSuccessRate(currentReport) }}%</el-descriptions-item>
                <el-descriptions-item label="连接成功率">{{ currentReport.connectionSuccessRate }}%</el-descriptions-item>
                <el-descriptions-item label="平均连接时间">{{ currentReport.averageConnectionTime }}ms</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="report-section">
              <h3>错误统计</h3>
              <el-descriptions column="1" border>
                <el-descriptions-item label="超时消息数">{{ currentReport.timeoutCount }}</el-descriptions-item>
                <el-descriptions-item label="连接错误">
                  <div v-for="(error, index) in currentReport.connectionErrors" :key="index" class="error-item">
                    {{ error.type }}: {{ error.count }}次
                  </div>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="24">
            <div class="report-section">
              <h3>响应时间分布</h3>
              <div class="chart-container">
                <canvas id="reportResponseTimeChart"></canvas>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="reportDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadReport(currentReport.taskId)">下载报告</el-button>
      </div>
    </el-dialog>

    <!-- 通知提示 -->
    <el-notification
      :title="notification.title"
      :message="notification.message"
      :type="notification.type"
      :visible.sync="notification.visible"
      :duration="notification.duration || 3000"
    ></el-notification>
  </div>
</template>

<script>
import axios from 'axios';
import Chart from 'chart.js';
import 'element-ui/lib/theme-chalk/index.css';

export default {
  name: 'FixTestDashboard',
  data() {
    return {
      // 菜单状态
      activeMenu: 'dashboard',
      activeSubMenu: 'new-test',
      
      // 测试参数
      testParams: {
        sessions: 10,
        rate: 50,
        messages: 1000,
        duration: 5,
        timeout: 5,
        configPath: 'fixconfig.template',
        detailedLog: false,
        recordSystemMetrics: true
      },
      testMode: 'messageCount',
      isStartingTest: false,
      
      // 测试状态
      currentTest: null,
      testLogs: [],
      testReports: [],
      currentReport: null,
      reportDialogVisible: false,
      
      // 系统状态
      systemStatus: {
        engineConnected: true,
        activeTests: 0,
        systemLoad: 35
      },
      
      // 分页和搜索
      searchTestId: '',
      currentPage: 1,
      pageSize: 10,
      loadingTestReports: false,
      
      // 图表实例
      charts: {},
      
      // 通知
      notification: {
        visible: false,
        title: '',
        message: '',
        type: 'info'
      }
    };
  },
  
  computed: {
    // 测试参数验证规则
    testRules() {
      return {
        sessions: [
          { required: true, message: '请输入会话数量', trigger: 'blur' }
        ],
        rate: [
          { required: true, message: '请输入消息速率', trigger: 'blur' }
        ],
        messages: [
          { required: this.testMode === 'messageCount', message: '请输入消息总数', trigger: 'blur' }
        ],
        duration: [
          { required: this.testMode === 'duration', message: '请输入持续时间', trigger: 'blur' }
        ],
        timeout: [
          { required: true, message: '请输入超时时间', trigger: 'blur' }
        ],
        configPath: [
          { required: true, message: '请输入配置文件路径', trigger: 'blur' }
        ]
      };
    },
    
    // 过滤测试报告
    filteredTestReports() {
      if (!this.searchTestId) {
        return this.testReports;
      }
      return this.testReports.filter(report => 
        report.taskId.includes(this.searchTestId)
      );
    }
  },
  
  mounted() {
    // 初始化页面
    this.loadTestReports();
    this.initSystemMonitoring();
    
    // 模拟WebSocket连接，实时获取测试状态
    this.setupTestStatusPolling();
  },
  
  methods: {
    // 菜单切换
    handleSubMenuSelect(key) {
      this.activeSubMenu = key;
      
      // 如果切换到当前测试或系统监控，初始化图表
      if (key === 'current-test' && this.currentTest) {
        this.initResponseTimeChart();
      } else if (key === 'system-monitor') {
        this.initSystemCharts();
      }
    },
    
    // 切换到新建测试
    switchToNewTest() {
      this.activeSubMenu = 'new-test';
    },
    
    // 测试模式切换
    handleTestModeChange(mode) {
      this.testMode = mode;
    },
    
    // 开始测试
    startTest() {
      this.$refs.testForm.validate((valid) => {
        if (valid) {
          this.isStartingTest = true;
          
          // 构建请求参数
          const requestParams = {
            sessions: this.testParams.sessions,
            rate: this.testParams.rate,
            timeout: this.testParams.timeout,
            config_path: this.testParams.configPath,
            detailed_log: this.testParams.detailedLog,
            record_system_metrics: this.testParams.recordSystemMetrics
          };
          
          // 根据测试模式添加相应参数
          if (this.testMode === 'messageCount') {
            requestParams.messages = this.testParams.messages;
          } else {
            requestParams.duration = this.testParams.duration;
          }
          
          // 调用API启动测试
          axios.post('/api/fix-test/start', requestParams)
            .then(response => {
              this.isStartingTest = false;
              if (response.data.taskId) {
                this.showNotification('测试已启动', `任务ID: ${response.data.taskId}`, 'success');
                this.activeSubMenu = 'current-test';
                
                // 延迟获取测试状态
                setTimeout(() => {
                  this.getCurrentTestStatus(response.data.taskId);
                }, 1000);
              }
            })
            .catch(error => {
              this.isStartingTest = false;
              this.showNotification('启动失败', error.response?.data?.message || '无法启动测试，请稍后重试', 'error');
              console.error('启动测试失败:', error);
            });
        }
      });
    },
    
    // 重置表单
    resetForm() {
      this.$refs.testForm.resetFields();
    },
    
    // 加载模板
    loadTemplate() {
      this.$confirm('确定要加载默认测试模板吗？当前配置将被覆盖', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 加载默认模板
        this.testParams = {
          sessions: 20,
          rate: 100,
          messages: 5000,
          duration: 10,
          timeout: 5,
          configPath: 'fixconfig.template',
          detailedLog: true,
          recordSystemMetrics: true
        };
        this.testMode = 'messageCount';
        this.showNotification('模板加载成功', '已加载默认测试参数模板', 'success');
      });
    },
    
    // 停止测试
    stopTest() {
      if (!this.currentTest) return;
      
      this.$confirm(`确定要终止测试 ${this.currentTest.taskId} 吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        axios.post(`/api/fix-test/stop/${this.currentTest.taskId}`)
          .then(response => {
            this.showNotification('测试已终止', `测试 ${this.currentTest.taskId} 已成功终止`, 'info');
          })
          .catch(error => {
            this.showNotification('终止失败', error.response?.data?.message || '无法终止测试，请稍后重试', 'error');
          });
      });
    },
    
    // 获取当前测试状态
    getCurrentTestStatus(taskId) {
      axios.get(`/api/fix-test/status/${taskId}`)
        .then(response => {
          this.currentTest = response.data;
          
          // 如果测试正在运行，初始化图表
          if (this.currentTest.status === 'RUNNING' && this.activeSubMenu === 'current-test') {
            this.initResponseTimeChart();
          }
          
          // 获取测试日志
          this.getTestLogs(taskId);
        })
        .catch(error => {
          console.error('获取测试状态失败:', error);
        });
    },
    
    // 获取测试日志
    getTestLogs(taskId) {
      axios.get(`/api/fix-test/logs/${taskId}`)
        .then(response => {
          this.testLogs = response.data.logs || [];
          
          // 自动滚动到底部
          const scrollbar = document.querySelector('.log-container .el-scrollbar__wrap');
          if (scrollbar) {
            scrollbar.scrollTop = scrollbar.scrollHeight;
          }
        })
        .catch(error => {
          console.error('获取测试日志失败:', error);
        });
    },
    
    // 加载测试报告历史
    loadTestReports() {
      this.loadingTestReports = true;
      axios.get('/api/fix-test/reports')
        .then(response => {
          this.testReports = response.data || [];
          this.loadingTestReports = false;
        })
        .catch(error => {
          console.error('加载测试报告失败:', error);
          this.loadingTestReports = false;
        });
    },
    
    // 查看测试报告详情
    viewTestReport(taskId) {
      axios.get(`/api/fix-test/report/${taskId}`)
        .then(response => {
          this.currentReport = response.data;
          this.reportDialogVisible = true;
          
          // 延迟初始化图表，确保DOM已更新
          setTimeout(() => {
            this.initReportCharts();
          }, 100);
        })
        .catch(error => {
          console.error('获取测试报告失败:', error);
          this.showNotification('获取报告失败', '无法加载测试报告，请稍后重试', 'error');
        });
    },
    
    // 下载测试报告
    downloadReport(taskId) {
      axios.get(`/api/fix-test/report/${taskId}/download`, { responseType: 'blob' })
        .then(response => {
          const url = window.URL.createObjectURL(new Blob([response.data]));
          const link = document.createElement('a');
          link.href = url;
          link.setAttribute('download', `fix-test-report-${taskId}.json`);
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
          
          this.showNotification('下载成功', '测试报告已开始下载', 'success');
        })
        .catch(error => {
          console.error('下载测试报告失败:', error);
          this.showNotification('下载失败', '无法下载测试报告，请稍后重试', 'error');
        });
    },
    
    // 分页处理
    handleSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
    },
    
    handleCurrentChange(val) {
      this.currentPage = val;
    },
    
    // 格式化时间
    formatTime(timestamp) {
      if (!timestamp) return '-';
      const date = new Date(timestamp);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    },
    
    // 格式化持续时间
    formatDuration(startTime, endTime) {
      if (!startTime || !endTime) return '-';
      const durationMs = endTime - startTime;
      const seconds = Math.floor(durationMs / 1000) % 60;
      const minutes = Math.floor(durationMs / (1000 * 60)) % 60;
      const hours = Math.floor(durationMs / (1000 * 60 * 60));
      
      return `${hours}小时${minutes}分钟${seconds}秒`;
    },
    
    // 获取状态标签类型
    getStatusTagType(status) {
      switch(status) {
        case 'RUNNING':
          return 'warning';
        case 'COMPLETED':
          return 'success';
        case 'FAILED':
          return 'danger';
        case 'STOPPED':
          return 'info';
        default:
          return 'default';
      }
    },
    
    // 获取系统负载颜色
    getLoadColor(load) {
      if (load < 30) return '#13ce66';
      if (load < 70) return '#ff7d00';
      return '#ff4949';
    },
    
    // 计算响应成功率
    calculateSuccessRate(report) {
      if (!report.totalMessagesSent) return 0;
      const successCount = report.totalResponsesReceived || 0;
      return ((successCount / report.totalMessagesSent) * 100).toFixed(2);
    },
    
    // 显示通知
    showNotification(title, message, type = 'info', duration = 3000) {
      this.notification = {
        visible: true,
        title,
        message,
        type,
        duration
      };
    },
    
    // 初始化响应时间图表
    initResponseTimeChart() {
      const ctx = document.getElementById('responseTimeChart').getContext('2d');
      
      // 如果已有图表实例，先销毁
      if (this.charts.responseTimeChart) {
        this.charts.responseTimeChart.destroy();
      }
      
      this.charts.responseTimeChart = new Chart(ctx, {
        type: 'line',
        data: {
          labels: [],
          datasets: [
            {
              label: '平均响应时间(ms)',
              data: [],
              borderColor: '#409EFF',
              backgroundColor: 'rgba(64, 158, 255, 0.1)',
              tension: 0.3,
              fill: true
            },
            {
              label: '95%响应时间(ms)',
              data: [],
              borderColor: '#ff7d00',
              borderDash: [5, 5],
              tension: 0.3,
              fill: false
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            x: {
              title: {
                display: true,
                text: '时间'
              }
            },
            y: {
              title: {
                display: true,
                text: '响应时间(ms)'
              },
              beginAtZero: true
            }
          },
          plugins: {
            legend: {
              position: 'top'
            }
          }
        }
      });
    },
    
    // 初始化系统监控图表
    initSystemCharts() {
      // CPU图表
      const cpuCtx = document.getElementById('cpuChart').getContext('2d');
      if (this.charts.cpuChart) {
        this.charts.cpuChart.destroy();
      }
      this.charts.cpuChart = new Chart(cpuCtx, {
        type: 'line',
        data: {
          labels: [],
          datasets: [{
            label: 'CPU使用率(%)',
            data: [],
            borderColor: '#ff4949',
            backgroundColor: 'rgba(255, 73, 73, 0.1)',
            tension: 0.3,
            fill: true
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: {
              min: 0,
              max: 100,
              title: {
                display: true,
                text: '使用率(%)'
              }
            }
          }
        }
      });
      
      // 内存图表
      const memoryCtx = document.getElementById('memoryChart').getContext('2d');
      if (this.charts.memoryChart) {
        this.charts.memoryChart.destroy();
      }
      this.charts.memoryChart = new Chart(memoryCtx, {
        type: 'line',
        data: {
          labels: [],
          datasets: [{
            label: '内存使用率(%)',
            data: [],
            borderColor: '#13ce66',
            backgroundColor: 'rgba(19, 206, 102, 0.1)',
            tension: 0.3,
            fill: true
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: {
              min: 0,
              max: 100,
              title: {
                display: true,
                text: '使用率(%)'
              }
            }
          }
        }
      });
      
      // 网络图表
      const networkCtx = document.getElementById('networkChart').getContext('2d');
      if (this.charts.networkChart) {
        this.charts.networkChart.destroy();
      }
      this.charts.networkChart = new Chart(networkCtx, {
        type: 'line',
        data: {
          labels: [],
          datasets: [
            {
              label: '发送(Kbps)',
              data: [],
              borderColor: '#409EFF',
              backgroundColor: 'transparent',
              tension: 0.3
            },
            {
              label: '接收(Kbps)',
              data: [],
              borderColor: '#722ed1',
              backgroundColor: 'transparent',
              tension: 0.3
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: {
              beginAtZero: true,
              title: {
                display: true,
                text: '速率(Kbps)'
              }
            }
          }
        }
      });
      
      // 会话图表
      const sessionsCtx = document.getElementById('sessionsChart').getContext('2d');
      if (this.charts.sessionsChart) {
        this.charts.sessionsChart.destroy();
      }
      this.charts.sessionsChart = new Chart(sessionsCtx, {
        type: 'bar',
        data: {
          labels: ['已连接', '连接中', '已断开', '失败'],
          datasets: [{
            label: '会话数量',
            data: [0, 0, 0, 0],
            backgroundColor: [
              'rgba(19, 206, 102, 0.7)',
              'rgba(255, 125, 0, 0.7)',
              'rgba(64, 158, 255, 0.7)',
              'rgba(255, 73, 73, 0.7)'
            ]
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
                text: '数量'
              }
            }
          }
        }
      });
    },
    
    // 初始化报告图表
    initReportCharts() {
      const ctx = document.getElementById('reportResponseTimeChart').getContext('2d');
      
      if (this.charts.reportResponseTimeChart) {
        this.charts.reportResponseTimeChart.destroy();
      }
      
      // 模拟响应时间分布数据
      const labels = ['0-10ms', '10-50ms', '50-100ms', '100-200ms', '200-500ms', '500ms以上'];
      const data = [35, 25, 15, 10, 8, 7]; // 示例数据
      
      this.charts.reportResponseTimeChart = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [{
            label: '消息数量占比(%)',
            data: data,
            backgroundColor: [
              'rgba(19, 206, 102, 0.7)',
              'rgba(147, 235, 93, 0.7)',
              'rgba(255, 210, 63, 0.7)',
              'rgba(255, 159, 64, 0.7)',
              'rgba(255, 99, 132, 0.7)',
              'rgba(221, 46, 68, 0.7)'
            ],
            borderWidth: 1
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
                text: '占比(%)'
              }
            }
          }
        }
      });
    },
    
    // 设置测试状态轮询
    setupTestStatusPolling() {
      // 每3秒检查一次测试状态
      setInterval(() => {
        // 如果有当前测试，更新状态
        if (this.currentTest) {
          this.getCurrentTestStatus(this.currentTest.taskId);
          
          // 如果测试正在运行，更新图表数据
          if (this.currentTest.status === 'RUNNING' && this.charts.responseTimeChart) {
            const now = new Date().toLocaleTimeString();
            const chart = this.charts.responseTimeChart;
            
            // 限制数据点数量
            if (chart.data.labels.length >= 30) {
              chart.data.labels.shift();
              chart.data.datasets[0].data.shift();
              chart.data.datasets[1].data.shift();
            }
            
            // 添加新数据点
            chart.data.labels.push(now);
            chart.data.datasets[0].data.push(this.currentTest.stats?.averageResponseTime || 0);
            chart.data.datasets[1].data.push(this.currentTest.stats?.p95ResponseTime || 0);
            chart.update();
          }
        }
        
        // 如果在系统监控页面，更新系统指标
        if (this.activeSubMenu === 'system-monitor' && this.charts.cpuChart) {
          this.updateSystemMetrics();
        }
      }, 3000);
    },
    
    // 初始化系统监控
    initSystemMonitoring() {
      // 模拟系统监控数据
      this.systemMetrics = {
        cpu: [],
        memory: [],
        networkSend: [],
        networkReceive: [],
        sessionStats: [0, 0, 0, 0],
        timestamps: []
      };
    },
    
    // 更新系统指标
    updateSystemMetrics() {
      const now = new Date().toLocaleTimeString();
      
      // 模拟系统指标数据
      const newCpu = Math.min(100, Math.max(0, this.systemStatus.systemLoad + (Math.random() * 10 - 5)));
      const newMemory = Math.min(100, Math.max(30, 60 + (Math.random() * 20 - 10)));
      const newNetworkSend = Math.min(1000, Math.max(100, 500 + (Math.random() * 300 - 150)));
      const newNetworkReceive = Math.min(1000, Math.max(100, 400 + (Math.random() * 200 - 100)));
      
      // 更新系统状态
      this.systemStatus.systemLoad = newCpu;
      if (this.currentTest && this.currentTest.status === 'RUNNING') {
        this.systemStatus.activeTests = 1;
      } else {
        this.systemStatus.activeTests = 0;
      }
      
      // 更新图表数据
      if (this.charts.cpuChart) {
        // 限制数据点数量
        if (this.charts.cpuChart.data.labels.length >= 30) {
          this.charts.cpuChart.data.labels.shift();
          this.charts.cpuChart.data.datasets[0].data.shift();
          
          this.charts.memoryChart.data.labels.shift();
          this.charts.memoryChart.data.datasets[0].data.shift();
          
          this.charts.networkChart.data.labels.shift();
          this.charts.networkChart.data.datasets[0].data.shift();
          this.charts.networkChart.data.datasets[1].data.shift();
        }
        
        // 添加新数据
        this.charts.cpuChart.data.labels.push(now);
        this.charts.cpuChart.data.datasets[0].data.push(newCpu);
        this.charts.cpuChart.update();
        
        this.charts.memoryChart.data.labels.push(now);
        this.charts.memoryChart.data.datasets[0].data.push(newMemory);
        this.charts.memoryChart.update();
        
        this.charts.networkChart.data.labels.push(now);
        this.charts.networkChart.data.datasets[0].data.push(newNetworkSend);
        this.charts.networkChart.data.datasets[1].data.push(newNetworkReceive);
        this.charts.networkChart.update();
        
        // 更新会话统计
        if (this.currentTest) {
          this.charts.sessionsChart.data.datasets[0].data = [
            this.currentTest.stats?.totalSessions || 0,
            0,
            0,
            (this.currentTest.stats?.totalSessions || 0) * (100 - this.currentTest.stats?.connectionSuccessRate || 0) / 100
          ];
          this.charts.sessionsChart.update();
        }
      }
    }
  }
};
</script>

<style scoped>
.fix-test-dashboard {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.header {
  background-color: #1f2d3d;
  color: #fff;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.logo {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.logo i {
  font-size: 24px;
  margin-right: 10px;
  color: #409EFF;
}

.main-menu {
  flex: 1;
  margin: 0 20px;
}

.user-info {
  display: flex;
  align-items: center;
}

.username {
  margin-left: 10px;
}

.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.sidebar {
  background-color: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  padding: 20px;
  overflow-y: auto;
}

.sidebar-menu {
  border-radius: 4px;
  background-color: transparent;
}

.system-status-card {
  margin-top: 20px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #e4e7ed;
}

.status-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.content-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f5f7fa;
}

.content-card {
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.test-form {
  margin-top: 20px;
}

.el-row {
  margin-bottom: 15px;
}

.form-actions {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.empty-state {
  text-align: center;
  padding: 50px 0;
  color: #909399;
}

.empty-state i {
  font-size: 50px;
  margin-bottom: 20px;
  color: #c0c4cc;
}

.test-monitoring {
  margin-top: 20px;
}

.stat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.stat-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.progress-container {
  padding: 10px 0;
}

.progress-stats {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 14px;
}

.performance-stats {
  display: flex;
  justify-content: space-around;
  padding: 10px 0;
}

.performance-item {
  text-align: center;
  flex: 1;
}

.performance-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

.performance-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 300px;
  position: relative;
}

.log-container {
  padding: 10px;
  background-color: #fafafa;
  border-radius: 4px;
}

.log-entry {
  padding: 5px 0;
  border-bottom: 1px dashed #e4e7ed;
  font-size: 13px;
  display: flex;
  align-items: center;
}

.log-entry:last-child {
  border-bottom: none;
}

.log-time {
  color: #909399;
  width: 120px;
  flex-shrink: 0;
}

.log-level {
  width: 80px;
  text-align: center;
  font-weight: bold;
  flex-shrink: 0;
}

.level-info {
  color: #409EFF;
}

.level-warning {
  color: #e6a23c;
}

.level-error {
  color: #f56c6c;
}

.level-success {
  color: #67c23a;
}

.log-message {
  flex: 1;
  margin-left: 10px;
}

.error-log {
  background-color: rgba(245, 108, 108, 0.05);
}

.search-input {
  width: 250px;
  float: right;
}

.report-details {
  margin-top: 10px;
}

.report-section {
  margin-bottom: 20px;
}

.report-section h3 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #303133;
  font-weight: bold;
}

.error-item {
  margin-bottom: 5px;
}

@media (max-width: 1200px) {
  .performance-stats {
    flex-direction: column;
  }
  
  .performance-item {
    margin-bottom: 15px;
  }
}
</style>
