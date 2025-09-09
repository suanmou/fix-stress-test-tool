// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/views/Layout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '测试监控' }
      },
      {
        path: 'test-plan',
        name: 'TestPlan',
        component: () => import('@/views/TestPlan.vue'),
        meta: { title: '测试计划' }
      },
      {
        path: 'test-plan/create',
        name: 'CreateTestPlan',
        component: () => import('@/views/CreateTestPlan.vue'),
        meta: { title: '创建测试计划' }
      },
      {
        path: 'test-task',
        name: 'TestTask',
        component: () => import('@/views/TestTask.vue'),
        meta: { title: '测试任务' }
      },
      {
        path: 'monitor/:taskId',
        name: 'Monitor',
        component: () => import('@/views/Monitor.vue'),
        meta: { title: '实时监控' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router