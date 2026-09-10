import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    const res = error.response && error.response.data
    if (res && res.message) {
      return Promise.reject(new Error(res.message))
    }
    return Promise.reject(new Error(error.message || '网络异常，请稍后重试'))
  }
)

export default request
