import request from './request'

export const getBenchSponsorships = (params) => request.get('/bench-sponsorships', { params })
export const createBenchSponsorship = (data) => request.post('/bench-sponsorships', data)
