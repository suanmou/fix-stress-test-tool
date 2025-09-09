// src/stores/testPlan.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { testPlanApi } from '@/api'

export const useTestPlanStore = defineStore('testPlan', () => {
  const testPlans = ref([])
  const currentPlan = ref(null)
  const loading = ref(false)
  
  const totalPlans = computed(() => testPlans.value.length)
  
  const fetchTestPlans = async () => {
    loading.value = true
    try {
      const response = await testPlanApi.getAll()
      testPlans.value = response.data || []
    } finally {
      loading.value = false
    }
  }
  
  const createTestPlan = async (planData) => {
    const response = await testPlanApi.create(planData)
    if (response.code === 200) {
      testPlans.value.unshift(response.data)
      return response.data
    }
    throw new Error(response.message)
  }
  
  const getTestPlan = async (planId) => {
    const response = await testPlanApi.getById(planId)
    if (response.code === 200) {
      currentPlan.value = response.data
      return response.data
    }
    throw new Error(response.message)
  }
  
  return {
    testPlans,
    currentPlan,
    loading,
    totalPlans,
    fetchTestPlans,
    createTestPlan,
    getTestPlan
  }
})