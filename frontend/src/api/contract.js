import request from '@/utils/request'

/**
 * 上传合同文件
 */
export function uploadContract(file, receivableId, description) {
  const formData = new FormData()
  formData.append('file', file)
  if (receivableId) {
    formData.append('receivableId', receivableId)
  }
  if (description) {
    formData.append('description', description)
  }
  return request({
    url: '/contracts/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取合同信息
 */
export function getContractInfo(id) {
  return request({
    url: `/contracts/${id}`,
    method: 'get'
  })
}

/**
 * 获取合同下载链接
 */
export function getContractDownloadUrl(id) {
  return `/api/contracts/${id}/download`
}

/**
 * 验证合同（哈希校验）
 */
export function verifyContract(id) {
  return request({
    url: `/contracts/${id}/verify`,
    method: 'get'
  })
}

/**
 * 根据应收账款获取关联合同
 */
export function getContractsByReceivable(receivableId) {
  return request({
    url: `/contracts/receivable/${receivableId}`,
    method: 'get'
  })
}

/**
 * 获取我的合同列表
 */
export function getMyContracts() {
  return request({
    url: '/contracts/my',
    method: 'get'
  })
}

/**
 * 通过哈希查找合同
 */
export function getContractByHash(hash) {
  return request({
    url: `/contracts/hash/${hash}`,
    method: 'get'
  })
}

/**
 * 删除合同
 */
export function deleteContract(id) {
  return request({
    url: `/contracts/${id}`,
    method: 'delete'
  })
}
