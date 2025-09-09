<!-- src/views/TestTask.vue -->
<template>
  <div class="test-task-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>测试任务管理</span>
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            创建任务
          </el-button>
        </div>
      </template>
      
      <!-- 任务列表 -->
      <el-table :data="taskList" style="width: 100%" v-loading="loading">
        <el-table-column prop="taskId" label="任务ID" width="180" />
        <el-table-column prop="planName" label="测试计划" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentTps" label="当前TPS" width="100" />
        <el-table-column prop="progress" label="进度" width="200">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.progress" 
              :status="getProgressStatus(row.status)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button 
                v-if="row.status === 'STARTING'" 
                type="success" 
                size="small"
                @click="startTask(row.taskId)">
                启动
              </el-button>
              <el-button 
                v-if="row.status === 'RUNNING'" 
                type="warning" 
                size="small"
                @click="pauseTask(row.taskId)">
                暂停
              </el-button>
              <el-button 
                v-if="row.status === 'PAUSED'" 
                type="primary" 
                size="small"
                @click="resumeTask(row.taskId)">
                恢复
              </el-button>
              <el-button 
                v-if="['RUNNING', 'PAUSED'].includes(row.status)" 
                type="danger" 
                size="small"
                @click="stopTask(row.taskId)">
                停止
              </el-button>
              <el-button 
                type="info" 
                size="small"
                @click="viewDetail(row.taskId)">
                详情
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchTasks"
      />
    </el-card>
    
    <!-- 创建任务对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建测试任务" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="测试计划">
          <el-select v-model="createForm.planId" placeholder="选择测试计划">
            <el-option
              v-for="plan in testPlans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急令牌">
          <el-input v-model="createForm.emergencyToken" placeholder="用于紧急停止" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createTask">创建</el-button>
      </template>
    </el-dialog>
    
    <!-- 任务详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="任务详情" width="800px">
      <div v-if="taskDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ taskDetail.task.taskId }}</el-descriptions-item>
          <el-descriptions-item label="测试计划">{{ taskDetail.task.testPlan.planName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(taskDetail.task.status)">
              {{ getStatusText(taskDetail.task.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前TPS">{{ taskDetail.metrics.currentTps }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ taskDetail.task.startTime }}</el-descriptions-item>
          <el-descriptions-item label="运行时长">{{ formatDuration(taskDetail.task.totalDuration) }}</el-descriptions-item>
        </el-descriptions>
        
        <h4>步骤进度</h4>
        <el-timeline>
          <el-timeline-item
            v-for="step in taskDetail.steps"
            :key="step.stepNumber"
            :type="getStepType(step.status)"
            :timestamp="step.startTime"
          >
            步骤 {{ step.stepNumber }}: {{ step.targetTps }} TPS
            <el-progress :percentage="getStepProgress(step)" />
          </el-timeline-item>
        </el-timeline>
        
        <h4>实时监控</h4>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="已发送消息" :value="taskDetail.metrics.totalMessagesSent" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="已接收消息" :value="taskDetail.metrics.totalMessagesReceived" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="失败消息" :value="taskDetail.metrics.totalMessagesFailed" />
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { testTaskApi, testPlanApi } from '@/api'
import { useRouter } from 'vue-router'

const router = useRouter()

const taskList = ref([])
const testPlans = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const taskDetail = ref(null)

const createForm = ref({
  planId: null,
  emergencyToken: ''
})

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 获取任务列表
const fetchTasks = async () => {
  loading.value = true
  try {
    const response = await testTaskApi.getAll()
    taskList.value = response.data || []
    total.value = taskList.value.length
  } finally {
    loading.value = false
  }
}

// 获取测试计划列表
const fetchTestPlans = async () => {
  const response = await testPlanApi.getAll()
  testPlans.value = response.data || []
}

// 创建任务
const createTask = async () => {
  try {
    await testTaskApi.create(createForm.value.planId, {
      emergencyToken: createForm.value.emergencyToken
    })
    ElMessage.success('任务创建成功')
    showCreateDialog.value = false
    fetchTasks()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

// 启动任务
const startTask = async (taskId) => {
  try {
    await testTaskApi.start(taskId)
    ElMessage.success('任务启动成功')
    fetchTasks()
  } catch (error) {
    ElMessage.error('启动失败')
  }
}

// 暂停任务
const pauseTask = async (taskId) => {
  try {
    await testTaskApi.pause(taskId)
    ElMessage.success('任务已暂停')
    fetchTasks()
  } catch (error) {
    ElMessage.error('暂停失败')
  }
}

// 恢复任务
const resumeTask = async (taskId) => {
  try {
    await testTaskApi.resume(taskId)
    ElMessage.success('任务已恢复')
    fetchTasks()
  } catch (error) {
    ElMessage.error('恢复失败')
  }
}

// 停止任务
const stopTask = async (taskId) => {
  try {
    await ElMessageBox.confirm('确定要停止任务吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await testTaskApi.stop(taskId, { emergencyToken: 'emergency-token' })
    ElMessage.success('任务已停止')
    fetchTasks()
  } catch (error) {
    // 用户取消不处理
  }
}

// 查看详情
const viewDetail = async (taskId) => {
  const response = await testTaskApi.getById(taskId)
  taskDetail.value = response.data
  showDetailDialog.value = true
}

// 工具函数
const getStatusType = (status) => {
  const types = {
    'STARTING': 'info',
    'RUNNING': 'success',
    'PAUSED': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'STOPPING': 'warning'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'STARTING': '启动中',
    'RUNNING': '运行中',
    'PAUSED': '已暂停',
    'COMPLETED': '已完成',
    'FAILED': '已失败',
    'STOPPING': '停止中'
  }
  return texts[status] || status
}

const getProgressStatus = (status) => {
  const statuses = {
    'COMPLETED': 'success',
    'FAILED': 'exception'
  }
  return statuses[status]
}

const getStepType = (status) => {
  const types = {
    'PENDING': 'primary',
    'RUNNING': 'primary',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'SKIPPED': 'info'
  }
  return types[status] || 'primary'
}

const getStepProgress = (step) => {
  if (step.status === 'COMPLETED') return 100
  if (step.status === 'RUNNING') return 50
  return 0
}

const formatDuration = (duration) => {
  if (!duration) return '0s'
  const seconds = Math.floor(duration / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) return `${hours}h ${minutes % 60}m ${seconds % 60}s`
  if (minutes > 0) return `${minutes}m ${seconds % 60}s`
  return `${seconds}s`
}

onMounted(() => {
  fetchTasks()
  fetchTestPlans()
  
  // 定时刷新
  setInterval(fetchTasks, 5000)
})
</script>

<style scoped>
.test-task-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>