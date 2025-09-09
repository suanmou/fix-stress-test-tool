<!-- src/views/Monitor.vue -->
<template>
  <div class="monitor-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="metric-card">
          <template #header>
            <div class="card-header">
              <span>当前TPS</span>
              <el-icon><TrendCharts /></el-icon>
            </div>
          </template>
          <div class="metric-value">{{ metrics.currentTps || 0 }}</div>
          <div class="metric-unit">消息/秒</div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card">
          <template #header>
            <div class="card-header">
              <span>会话数量</span>
              <el-icon><Connection /></el-icon>
            </div>
          </template>
          <div class="metric-value">{{ metrics.sessionCount || 0 }}</div>
          <div class="metric-unit">个</div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card">
          <template #header>
            <div class="card-header">
              <span>已发送消息</span>
              <el-icon><Upload /></el-icon>
            </div>
          </template>
          <div class="metric-value">{{ formatNumber(metrics.totalSent || 0) }}</div>
          <div class="metric-unit">条</div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card">
          <template #header>
            <div class="card-header">
              <span>失败消息</span>
              <el-icon><Warning /></el-icon>
            </div>
          </template>
          <div class="metric-value" :class="{ 'error': metrics.totalFailed > 0 }">
            {{ formatNumber(metrics.totalFailed || 0) }}
          </div>
          <div class="metric-unit">条</div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>TPS实时趋势</span>
            </div>
          </template>
          <div ref="tpsChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>消息发送状态</span>
            </div>
          </template>
          <div ref="statusChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>测试控制</span>
            </div>
          </template>
          <el-space>
            <el-button 
              v-if="taskStatus === 'RUNNING'" 
              type="warning" 
              @click="pauseTest"
              :loading="loading">
              暂停测试
            </el-button>
            <el-button 
              v-if="taskStatus === 'PAUSED'" 
              type="primary" 
              @click="resumeTest"
              :loading="loading">
              恢复测试
            </el-button>
            <el-button 
              type="danger" 
              @click="stopTest"
              :loading="loading">
              停止测试
            </el-button>
          </el-space>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { testTaskApi, monitorApi } from '@/api'
import { formatNumber } from '@/utils'

const route = useRoute()
const taskId = route.params.taskId

const metrics = ref({})
const taskStatus = ref('RUNNING')
const loading = ref(false)

const tpsChart = ref()
const statusChart = ref()
let tpsChartInstance = null
let statusChartInstance = null
let timer = null

const tpsData = []
const timeLabels = []

// 初始化图表
const initCharts = () => {
  tpsChartInstance = echarts.init(tpsChart.value)
  statusChartInstance = echarts.init(statusChart.value)
  
  // TPS趋势图配置
  tpsChartInstance.setOption({
    title: { text: 'TPS实时趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: timeLabels },
    yAxis: { type: 'value', name: 'TPS' },
    series: [{
      data: tpsData,
      type: 'line',
      smooth: true,
      areaStyle: {}
    }]
  })
  
  // 状态饼图配置
  statusChartInstance.setOption({
    title: { text: '消息状态分布' },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: [
        { value: 0, name: '已发送' },
        { value: 0, name: '已接收' },
        { value: 0, name: '失败' }
      ]
    }]
  })
}

// 获取监控数据
const fetchMetrics = async () => {
  try {
    const response = await monitorApi.getMetrics(taskId)
    if (response.code === 200) {
      metrics.value = response.data
      
      // 更新图表数据
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
      
      if (statusChartInstance) {
        statusChartInstance.setOption({
          series: [{
            data: [
              { value: response.data.totalSent || 0, name: '已发送' },
              { value: response.data.totalReceived || 0, name: '已接收' },
              { value: response.data.totalFailed || 0, name: '失败' }
            ]
          }]
        })
      }
    }
  } catch (error) {
    console.error('获取监控数据失败:', error)
  }
}

// 测试控制方法
const pauseTest = async () => {
  loading.value = true
  try {
    await testTaskApi.pause(taskId)
    taskStatus.value = 'PAUSED'
    ElMessage.success('测试已暂停')
  } catch (error) {
    ElMessage.error('暂停失败')
  } finally {
    loading.value = false
  }
}

const resumeTest = async () => {
  loading.value = true
  try {
    await testTaskApi.resume(taskId)
    taskStatus.value = 'RUNNING'
    ElMessage.success('测试已恢复')
  } catch (error) {
    ElMessage.error('恢复失败')
  } finally {
    loading.value = false
  }
}

const stopTest = async () => {
  loading.value = true
  try {
    await testTaskApi.stop(taskId, { emergencyToken: 'emergency-token' })
    taskStatus.value = 'STOPPED'
    ElMessage.success('测试已停止')
    clearInterval(timer)
  } catch (error) {
    ElMessage.error('停止失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initCharts()
  fetchMetrics()
  timer = setInterval(fetchMetrics, 1000)
  
  window.addEventListener('resize', () => {
    tpsChartInstance?.resize()
    statusChartInstance?.resize()
  })
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  tpsChartInstance?.dispose()
  statusChartInstance?.dispose()
})
</script>

<style scoped>
.monitor-container {
  padding: 20px;
}

.metric-card {
  text-align: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
}

.metric-unit {
  font-size: 14px;
  color: #909399;
}

.error {
  color: #F56C6C;
}
</style>