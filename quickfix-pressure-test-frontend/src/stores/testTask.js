// src/stores/testTask.js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { testTaskApi } from '@/api'

export const useTestTaskStore = defineStore('testTask', () => {
  const tasks = ref([])
  const currentTask = ref(null)
  const loading = ref(false)
  
  const fetchTasks = async () => {
    loading.value = true
    try {
      const response = await testTaskApi.getAll()
      tasks.value = response.data || []
    } finally {
      loading.value = false
    }
  }
  
  const createTask = async (planId, data) => {
    const response = await testTaskApi.create(planId, data)
    if (response.code === 200) {
      tasks.value.unshift(response.data)
      return response.data
    }
    throw new Error(response.message)
  }
  
  const startTask = async (taskId) => {
    await testTaskApi.start(taskId)
  }
  
  const pauseTask = async (taskId) => {
    await testTaskApi.pause(taskId)
  }
  
  const resumeTask = async (taskId) => {
    await testTaskApi.resume(taskId)
  }
  
  const stopTask = async (taskId, emergencyToken) => {
    await testTaskApi.stop(taskId, emergencyToken)
  }
  
  return {
    tasks,
    currentTask,
    loading,
    fetchTasks,
    createTask,
    startTask,
    pauseTask,
    resumeTask,
    stopTask
  }
})