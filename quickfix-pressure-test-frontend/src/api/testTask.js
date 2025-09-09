// src/api/testTask.js
import api from './index'

export const testTaskApi = {
  // 创建测试任务
  create: (planId, data) => api.post(`/test-task/create/${planId}`, data),
  
  // 启动任务
  start: (taskId) => api.post(`/test-task/${taskId}/start`),
  
  // 暂停任务
  pause: (taskId) => api.put(`/test-task/${taskId}/pause`),
  
  // 恢复任务
  resume: (taskId) => api.put(`/test-task/${taskId}/resume`),
  
  // 停止任务
  stop: (taskId, emergencyToken) => api.put(`/test-task/${taskId}/stop`, null, {
    params: { emergencyToken }
  }),
  
  // 获取任务列表
  getAll: () => api.get('/test-task'),
  
  // 获取任务详情
  getById: (taskId) => api.get(`/test-task/${taskId}`),
  
  // 获取任务指标
  getMetrics: (taskId) => api.get(`/test-task/${taskId}/metrics`)
}