import request from './request.js'

export function listMembershipPlans() {
  return request.get('/membership-plans')
}
