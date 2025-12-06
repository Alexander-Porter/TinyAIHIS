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
    // Handle blob responses (file downloads) - return raw data
    if (response.config.responseType === 'blob') {
      return response.data
    }
    
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
  adminLogin: (data) => api.post('/auth/staff/login', data), // alias for admin
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
  getTodayPatients: (doctorId) => api.get(`/doctor/patients/${doctorId}`),
  getVisitDetail: (regId) => api.get(`/doctor/visit/${regId}`),
  getPatientHistory: (patientId, doctorId) => api.get(`/doctor/history/${patientId}`, { params: { doctorId } }),
  pause: (regId) => api.post(`/doctor/pause/${regId}`),
  resume: (regId) => api.post(`/doctor/resume/${regId}`),
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

// Check Item APIs
export const checkItemApi = {
  getAll: () => api.get('/check-item/list'),
  search: (keyword) => api.get('/check-item/search', { params: { keyword } }),
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
  // User management
  getUsers: (params) => api.get('/admin/users', { params }),
  saveUser: (data) => api.post('/admin/user/save', data),
  updateUserStatus: (userId, status) => api.post(`/admin/user/${userId}/status`, null, { params: { status } }),
  deleteUser: (userId) => api.delete(`/admin/user/${userId}`),
  
  // Department management
  saveDepartment: (data) => api.post('/admin/department/save', data),
  updateDepartmentStatus: (deptId, status) => api.post(`/admin/department/${deptId}/status`, null, { params: { status } }),
  deleteDepartment: (deptId) => api.delete(`/admin/department/${deptId}`),
  
  // Consulting room management
  getRooms: () => api.get('/admin/rooms'),
  saveRoom: (data) => api.post('/admin/rooms', data),
  deleteRoom: (roomId) => api.delete(`/admin/room/${roomId}`),
  
  // Schedule template management (weekly recurring schedules)
  getScheduleTemplates: (deptId) => api.get('/admin/schedule-templates', { params: { deptId } }),
  saveScheduleTemplate: (data) => api.post('/admin/schedule-template/save', data),
  deleteScheduleTemplate: (templateId) => api.delete(`/admin/schedule-template/${templateId}`),
  generateWeekSchedules: () => api.post('/admin/schedules/generate'),
  
  // Drug import/export
  importDrugs: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/admin/drugs/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  exportDrugs: () => api.get('/admin/drugs/export', { responseType: 'blob' }),
  
  // Statistics
  getStats: () => api.get('/admin/stats'),
  getDashboardStats: () => api.get('/admin/dashboard-stats'),
  
  // Data Query & Export
  queryData: (params) => api.get('/admin/query', { params }),
  exportData: (params) => {
    return api.get('/admin/export', { params, responseType: 'blob' }).then(res => {
      const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${params.type}_export_${new Date().toISOString().slice(0,10)}.xlsx`
      a.click()
      window.URL.revokeObjectURL(url)
    })
  },
}

// Knowledge Base (admin)
export const kbApi = {
  list: () => api.get('/admin/kb/list'),
  get: (id) => api.get(`/admin/kb/${id}`),
  add: (data) => api.post('/admin/kb', data),
  update: (id, data) => api.put(`/admin/kb/${id}`, data),
  remove: (id) => api.delete(`/admin/kb/${id}`)
}
