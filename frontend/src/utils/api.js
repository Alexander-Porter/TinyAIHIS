import axios from 'axios'
import { message } from 'ant-design-vue'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// Request interceptor
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // Add user info headers for backend
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.userId) {
      config.headers['X-User-Id'] = userInfo.userId
      config.headers['X-Doctor-Id'] = userInfo.userId
    }
    
    return config
  },
  error => Promise.reject(error)
)

// Response interceptor
api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res.data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/'
    }
    message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default api

// Auth APIs
export const authApi = {
  patientRegister: (data) => api.post('/auth/patient/register', data),
  patientLogin: (data) => api.post('/auth/patient/login', data),
  staffLogin: (data) => api.post('/auth/staff/login', data),
  getPatient: (id) => api.get(`/auth/patient/${id}`),
  getStaff: (id) => api.get(`/auth/staff/${id}`),
}

// Schedule APIs
export const scheduleApi = {
  getDepartments: () => api.get('/schedule/departments'),
  getDoctors: (deptId) => api.get('/schedule/doctors', { params: { deptId } }),
  getScheduleList: (deptId, startDate, endDate) => 
    api.get('/schedule/list', { params: { deptId, startDate, endDate } }),
  saveSchedule: (data) => api.post('/schedule/save', data),
}

// Registration APIs
export const registrationApi = {
  create: (data) => api.post('/registration/create', data),
  get: (id) => api.get(`/registration/${id}`),
  getByPatient: (patientId) => api.get(`/registration/patient/${patientId}`),
  pay: (id) => api.post(`/registration/pay/${id}`),
  checkIn: (data) => api.post('/registration/checkin', data),
  cancel: (id) => api.post(`/registration/cancel/${id}`),
}

// Doctor APIs
export const doctorApi = {
  getQueue: (doctorId) => api.get(`/doctor/queue/${doctorId}`),
  callNext: (doctorId) => api.post(`/doctor/callNext/${doctorId}`),
  complete: (regId) => api.post(`/doctor/complete/${regId}`),
}

// EMR APIs
export const emrApi = {
  save: (data) => api.post('/emr/save', data),
  getByRecord: (recordId) => api.get(`/emr/${recordId}`),
  getByPatient: (patientId) => api.get(`/emr/patient/${patientId}`),
  getByReg: (regId) => api.get(`/emr/registration/${regId}`),
  getPrescriptions: (recordId) => api.get(`/emr/prescriptions/${recordId}`),
  getLabOrders: (recordId) => api.get(`/emr/laborders/${recordId}`),
  getTemplates: (deptId) => api.get('/emr/templates', { params: { deptId } }),
  saveTemplate: (data) => api.post('/emr/template/save', data),
  deleteTemplate: (id) => api.delete(`/emr/template/${id}`),
}

// Triage APIs
export const triageApi = {
  recommend: (data) => api.post('/triage/recommend', data),
}

// Queue APIs
export const queueApi = {
  getInfo: (deptId) => api.get(`/queue/${deptId}`),
  broadcast: (deptId) => api.post(`/queue/broadcast/${deptId}`),
}

// Lab APIs
export const labApi = {
  getPending: () => api.get('/lab/pending'),
  getOrder: (id) => api.get(`/lab/${id}`),
  pay: (id) => api.post(`/lab/pay/${id}`),
  submitResult: (data) => api.post('/lab/result', data),
  getByPatient: (patientId) => api.get(`/lab/patient/${patientId}`),
}

// Pharmacy APIs
export const pharmacyApi = {
  getDrugs: () => api.get('/pharmacy/drugs'),
  searchDrugs: (keyword) => api.get('/pharmacy/drugs/search', { params: { keyword } }),
  getPaidPrescriptions: () => api.get('/pharmacy/prescriptions/paid'),
  payPrescription: (id) => api.post(`/pharmacy/prescription/pay/${id}`),
  payByRecord: (recordId) => api.post(`/pharmacy/prescriptions/pay/record/${recordId}`),
  dispense: (id) => api.post(`/pharmacy/dispense/${id}`),
  updateStock: (drugId, quantity) => 
    api.post('/pharmacy/drug/stock', null, { params: { drugId, quantity } }),
  addDrug: (data) => api.post('/pharmacy/drug/add', data),
}

// Payment APIs
export const paymentApi = {
  payRegistration: (id) => api.post(`/payment/registration/${id}`),
  payPrescriptions: (recordId) => api.post(`/payment/prescription/record/${recordId}`),
  payLabOrder: (id) => api.post(`/payment/lab/${id}`),
}

// Admin APIs
export const adminApi = {
  importDrugs: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/admin/drugs/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  exportDrugs: () => api.get('/admin/drugs/export', { responseType: 'blob' }),
}
