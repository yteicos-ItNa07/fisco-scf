import request from '@/utils/request'

// 获取当前企业的流转记录
export function getMyTransfers() {
  return request.get('/transfers/my')
}

// 获取所有流转记录
export function getAllTransfers() {
  return request.get('/transfers')
}

// 获取通证相关的流转记录
export function getTransfersByReceivable(receivableId) {
  return request.get(`/transfers/receivable/${receivableId}`)
}

// 获取通证完整流转历史
export function getFullTransferHistory(tokenId) {
  return request.get(`/transfers/token/${tokenId}/history`)
}

// 获取指定企业的流转记录
export function getTransfersByEnterprise(enterpriseId) {
  return request.get(`/transfers/enterprise/${enterpriseId}`)
}
