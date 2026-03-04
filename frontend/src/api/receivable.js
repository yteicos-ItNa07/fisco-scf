import request from '@/utils/request'

// 签发应收账款（不需要传issuerId，后端从token获取）
export function issueReceivable(data) {
  return request.post('/receivables/issue', data)
}

// 拆分应收账款
export function splitReceivable(data) {
  return request.post('/receivables/split', data)
}

// 转让应收账款
export function transferReceivable(data) {
  return request.post('/receivables/transfer', data)
}

// 获取通证详情
export function getReceivableByTokenId(tokenId) {
  return request.get(`/receivables/token/${tokenId}`)
}

// 获取通证详情（包含流转历史）
export function getReceivableDetail(id) {
  return request.get(`/receivables/${id}/detail`)
}

// 获取当前企业持有的通证
export function getMyHoldingReceivables() {
  return request.get('/receivables/my/holding')
}

// 获取当前企业签发的通证
export function getMyIssuedReceivables() {
  return request.get('/receivables/my/issued')
}

// 获取所有可融资的应收账款（金融机构用）
export function getAvailableForFinancing() {
  return request.get('/receivables/available-for-financing')
}

// 获取持有的通证（兼容旧接口）
export function getReceivablesByHolder(holderId) {
  return request.get(`/receivables/holder/${holderId}`)
}

// 获取签发的通证（兼容旧接口）
export function getReceivablesByIssuer(issuerId) {
  return request.get(`/receivables/issuer/${issuerId}`)
}

// 风险评估
export function assessRisk(id) {
  return request.get(`/receivables/${id}/risk-assessment`)
}

// 申请融资
export function applyFinancing(data) {
  return request.post('/financing/apply', data)
}

// 审批融资
export function approveFinancing(data) {
  return request.post('/financing/approve', data)
}

// 放款
export function disburseFinancing(id) {
  return request.post(`/financing/${id}/disburse`)
}

// 获取当前企业的借款记录
export function getMyBorrowingRecords() {
  return request.get('/financing/my/borrowing')
}

// 获取当前企业签发的通证相关融资记录（核心企业）
export function getMyIssuedFinancing() {
  return request.get('/financing/my/issued')
}

// 获取当前企业的放款记录（金融机构）
export function getMyLendingRecords() {
  return request.get('/financing/my/lending')
}

// 获取待审批的融资申请（金融机构）
export function getMyPendingFinancing() {
  return request.get('/financing/my/pending')
}

// 获取融资记录（兼容旧接口）
export function getFinancingByBorrower(borrowerId) {
  return request.get(`/financing/borrower/${borrowerId}`)
}

// 获取待审批融资（兼容旧接口）
export function getPendingFinancing(financierId) {
  return request.get(`/financing/pending/${financierId}`)
}

// 获取融资详情
export function getFinancingById(id) {
  return request.get(`/financing/${id}`)
}

// 还款（借款方/供应商）
export function repayFinancing(id) {
  return request.post(`/financing/${id}/repay`)
}

// 到期结算（核心企业）
export function settleReceivable(id) {
  return request.post(`/receivables/${id}/settle`)
}
