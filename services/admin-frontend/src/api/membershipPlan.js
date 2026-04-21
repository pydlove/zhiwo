import request from './request.js'

export function listMembershipPlans() {
  return request.get('/membership-plans')
}

export function saveMembershipPlan(data) {
  if (data.id) {
    return request.put('/membership-plans/' + data.id, data)
  }
  return request.post('/membership-plans', data)
}

export function deleteMembershipPlan(id) {
  return request.delete('/membership-plans/' + id)
}
