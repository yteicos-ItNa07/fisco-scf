import request from '@/utils/request'

// 获取全局统计数据
export function getGlobalStats() {
  return request.get('/dashboard/stats')
}

// 获取当前企业的统计数据
export function getMyStats() {
  return request.get('/dashboard/my/stats')
}

// 获取指定企业的统计数据
export function getEnterpriseStats(enterpriseId) {
  return request.get(`/dashboard/enterprise/${enterpriseId}/stats`)
}
