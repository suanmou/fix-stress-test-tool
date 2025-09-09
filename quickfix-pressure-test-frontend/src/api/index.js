// src/api/index.js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
api.interceptors.response.use(
  response => response.data,
  error => {
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)

// 测试计划API
export const testPlanApi = {
  getAll: () => api.get('/test-plan'),
  getById: (id) => api.get(`/test-plan/${id}`),
  create: (data) => api.post('/test-plan', data),
  update: (id, data) => api.put(`/test-plan/${id}`, data),
  delete: (id) => api.delete(`/test-plan/${id}`)
}

// 测试任务API
export const testTaskApi = {
  start: (planId, data) => api.post(`/test-plan/${planId}/start`, data),
  pause: (taskId) => api.put(`/test-task/${taskId}/pause`),
  resume: (taskId) => api.put(`/test-task/${taskId}/resume`),
  stop: (taskId, data) => api.put(`/test-task/${taskId}/stop`, data),
  getAll: () => api.get('/test-task'),
  getById: (taskId) => api.get(`/test-task/${taskId}`)
}

// 监控API
export const monitorApi = {
  getMetrics: (taskId) => api.get(`/monitor/metrics/${taskId}`),
  checkEnv: (targetIp) => api.get('/system/env-check', { params: { targetEngineIp: targetIp } })
}

export default api