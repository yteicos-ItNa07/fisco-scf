import request from '@/utils/request'

export function getEnterprises() {
  return request.get('/enterprises')
}

export function getEnterpriseById(id) {
  return request.get(`/enterprises/${id}`)
}

export function getEnterprisesByRole(role) {
  return request.get(`/enterprises/role/${role}`)
}

export function registerEnterprise(data) {
  return request.post('/enterprises', data)
}

export function updateCreditScore(id, score) {
  return request.put(`/enterprises/${id}/credit-score`, null, { params: { score } })
}
