<!-- src/views/TaskExecution.vue -->
<template>
  <div class="task-execution-container">
    <!-- 任务信息 -->
    <el-card class="task-info-card">
      <template #header>
        <div class="card-header">
          <span>任务执行: {{ taskInfo?.taskId }}</span>
          <div class="task-controls">
            <el-button-group>
              <el-button 
                v-if="taskInfo?.status === 'RUNNING'" 
                type="warning" 
                @click="pauseExecution">
                <el-icon><VideoPause /></el-icon>
                暂停
              </el-button>
              <el-button 
                v-if="taskInfo?.status === 'PAUSED'" 
                type="primary" 
                @click="resumeExecution">
                <el-icon><VideoPlay /></el-icon>
                恢复
              </el-button>
              <el-button 
                type="danger" 
                @click="stopExecution">
                <el-icon><CircleClose /></el-icon>
                停止
              </el-button>
            </el-button-group>
          </div>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="当前TPS" :value="metrics?.currentTps || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="会话数量" :value="metrics?.sessionCount || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="已发送消息" :value="metrics?.totalMessagesSent || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="失败消息" :value="metrics?.totalMessagesFailed || 0" />
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 实时图表 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>TPS实时趋势</span>
          </template>
          <div ref="tpsChart" style="height: 400px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>消息速率</span>
          </template>
          <div ref="rateChart" style="height: 400px;"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 步骤进度 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>步骤进度</span>
      </template>
      <el-steps :active="currentStep" finish-status="success">
        <el-step
          v-for="step in steps"
          :key="step.stepNumber"
          :title="`步骤 ${step.stepNumber}`"
          :description="`${step.targetTps} TPS`"
        />
      </el-steps>
    </el-card>
    
    <!-- 实时日志 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>实时日志</span>
      </template>
      <div class="log-container" ref="logContainer">
        <div 
          v-for="(log, index) in logs" 
          :key="index" 
          class="log-item"
          :class="log.level"
        >
          [{{ log.timestamp }}] {{ log.message }}
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { testTaskApi, monitorApi } from '@/api'

const route = useRoute()
const taskId = route.params.taskId

const taskInfo = ref(null)
const metrics = ref(null)
const steps = ref([])
const currentStep = ref(0)
const logs = ref([])

const tpsChart = ref()
const rateChart = ref()
let tpsChartInstance = null
let rateChartInstance = null

const tpsData = []
const timeLabels = []

// 初始化图表
const initCharts = () => {
  tpsChartInstance = echarts.init(tpsChart.value)
  rateChartInstance = echarts.init(rateChart.value)
  
  // TPS图表
  tpsChartInstance.setOption({
    title: { text: 'TPS实时趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: timeLabels },
    yAxis: { type: 'value', name: 'TPS' },
    series: [{
      data: tpsData,
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
      lineStyle: { color: '#409EFF' }
    }]
  })
  
  // 消息速率图表
  rateChartInstance.setOption({
    title: { text: '消息发送速率' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['发送', '接收', '失败'] },
    xAxis: { type: 'category', data: timeLabels },
    yAxis: { type: 'value', name: '消息数' },
    series: [
      {
        name: '发送',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '接收',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '失败',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#F56C6C' }
      }
    ]
  })
}

// 获取任务信息
const fetchTaskInfo = async () => {
  const response = await testTaskApi.getById(taskId)
  taskInfo.value = response.data.task
  steps.value = response.data.steps || []
  currentStep.value = response.data.metrics?.currentStep || 0
}

// 获取实时指标
const fetchMetrics = async () => {
  const response = await testTaskApi.getMetrics(taskId)
  metrics.value = response.data
  
  // 更新图表
  if (tpsChartInstance) {
    const now = new Date().toLocaleTimeString()
    timeLabels.push(now)
    tpsData.push(response.data.currentTps || 0)
    
    if (timeLabels.length > 20) {
      timeLabels.shift()
      tpsData.shift()
    }
    
    tpsChartInstance.setOption({
      xAxis: { data: timeLabels },
      series: [{ data: tpsData }]
    })
  }
  
  if (rateChartInstance) {
    rateChartInstance.setOption({
      xAxis: { data: timeLabels },
      series: [
        { data: [...Array(timeLabels.length).fill(response.data.totalMessagesSent || 0)] },
        { data: [...Array(timeLabels.length).fill(response.data.totalMessagesReceived || 0)] },
        { data: [...Array(timeLabels.length).fill(response.data.totalMessagesFailed || 0)] }
      ]
    })
  }
}

// 控制方法
const pauseExecution = async () => {
  await testTaskApi.pause(taskId)
  addLog('任务已暂停', 'info')
}

const resumeExecution = async () => {
  await testTaskApi.resume(taskId)
  addLog('任务已恢复', 'info')
}

const stopExecution = async () => {
  await testTaskApi.stop(taskId, { emergencyToken: 'emergency-token' })
  addLog('任务已停止', 'warning')
}

// 添加日志
const addLog = (message, level = 'info') => {
  logs.value.unshift({
    timestamp: new Date().toLocaleTimeString(),
    message,
    level
  })
  
  if (logs.value.length > 100) {
    logs.value = logs.value.slice(0, 100)
  }
}

onMounted(() => {
  fetchTaskInfo()
  initCharts()
  
  // 定时刷新
  const timer = setInterval(() => {
    fetchMetrics()
    fetchTaskInfo()
  }, 1000)
  
  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    tpsChartInstance?.resize()
    rateChartInstance?.resize()
  })
  
  onUnmounted(() => {
    clearInterval(timer)
    tpsChartInstance?.dispose()
    rateChartInstance?.dispose()
  })
})
</script>

<style scoped>
.task-execution-container {
  padding: 20px;
}

.task-info-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.log-container {
  height: 300px;
  overflow-y: auto;
  background-color: #f5f5f5;
  padding: 10px;
  font-family: monospace;
  font-size: 12px;
}

.log-item {
  margin-bottom: 5px;
  padding: 2px 5px;
  border-radius: 3px;
}

.log-item.info {
  color: #409EFF;
}

.log-item.warning {
  color: #E6A23C;
}

.log-item.error {
  color: #F56C6C;
}

.log-item.success {
  color: #67C23A;
}
</style>