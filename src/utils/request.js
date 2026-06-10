import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',  // 根据实际情况修改
  timeout: 15000,
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
})

// 响应拦截器：直接返回 res.data，上层拿到后端 Result 对象
request.interceptors.response.use(
  (res) => res.data,
  (err) => Promise.reject(err)
)

export default request
