import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // Entry/Portal
  {
    path: '/',
    redirect: '/patient/login'
  },

  // Patient Routes (Mobile)
  {
    path: '/patient',
    component: () => import('@/views/patient/PatientLayout.vue'),
    children: [
      { path: '', redirect: '/patient/home' },
      { path: 'home', name: 'PatientHome', component: () => import('@/views/patient/Home.vue') },
      { path: 'login', name: 'PatientLogin', component: () => import('@/views/patient/Login.vue') },
      { path: 'register', name: 'PatientRegister', component: () => import('@/views/patient/Register.vue') },
      { path: 'triage', name: 'Triage', component: () => import('@/views/patient/Triage.vue') },
      { path: 'appointment', name: 'Appointment', component: () => import('@/views/patient/Appointment.vue') },
      { path: 'payment', name: 'Payment', component: () => import('@/views/patient/Payment.vue') },
      { path: 'checkin', name: 'CheckIn', component: () => import('@/views/patient/CheckIn.vue') },
      { path: 'reports', name: 'Reports', component: () => import('@/views/patient/Reports.vue') },
      { path: 'records', name: 'Records', component: () => import('@/views/patient/Records.vue') },
      { path: 'registration-records', name: 'RegistrationRecords', component: () => import('@/views/patient/RegistrationRecords.vue') },
    ]
  },

  // Doctor Routes (PC)
  {
    path: '/doctor',
    component: () => import('@/views/doctor/DoctorLayout.vue'),
    children: [
      { path: '', redirect: '/doctor/workstation' },
      { path: 'login', name: 'DoctorLogin', component: () => import('@/views/StaffLogin.vue') },
      { path: 'workstation', name: 'Workstation', component: () => import('@/views/doctor/Workstation.vue') },
      { path: 'templates', name: 'Templates', component: () => import('@/views/doctor/Templates.vue') },
    ]
  },

  // Lab Routes (PC)
  {
    path: '/lab',
    component: () => import('@/views/lab/LabLayout.vue'),
    children: [
      { path: '', redirect: '/lab/workstation' },
      { path: 'login', name: 'LabLogin', component: () => import('@/views/StaffLogin.vue') },
      { path: 'workstation', name: 'LabWorkstation', component: () => import('@/views/lab/Workstation.vue') },
    ]
  },

  // Pharmacy Routes (PC)
  {
    path: '/pharmacy',
    component: () => import('@/views/pharmacy/PharmacyLayout.vue'),
    children: [
      { path: '', redirect: '/pharmacy/dispense' },
      { path: 'login', name: 'PharmacyLogin', component: () => import('@/views/StaffLogin.vue') },
      { path: 'dispense', name: 'Dispense', component: () => import('@/views/pharmacy/Dispense.vue') },
      { path: 'inventory', name: 'Inventory', component: () => import('@/views/pharmacy/Inventory.vue') },
    ]
  },

  // Admin Routes (PC)
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'login', name: 'AdminLogin', component: () => import('@/views/StaffLogin.vue') },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', name: 'Users', component: () => import('@/views/admin/Users.vue') },
      { path: 'departments', name: 'Departments', component: () => import('@/views/admin/Departments.vue') },
      { path: 'rooms', name: 'Rooms', component: () => import('@/views/admin/Rooms.vue') },
      { path: 'schedules', name: 'Schedules', component: () => import('@/views/admin/Schedules.vue') },
      { path: 'kb', name: 'KnowledgeBase', component: () => import('@/views/admin/KnowledgeBase.vue') },
      { path: 'query', name: 'DataQuery', component: () => import('@/views/admin/DataQuery.vue') },
    ]
  },

  // Queue Screen (Fullscreen)
  {
    path: '/screen/:deptId',
    name: 'QueueScreen',
    component: () => import('@/views/screen/QueueScreen.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const publicPages = ['/', '/patient/login', '/patient/register', '/doctor/login', '/lab/login', '/pharmacy/login', '/admin/login']
  const isPublicPage = publicPages.includes(to.path) || to.path.startsWith('/screen/')

  // Check auth based on path
  if (!isPublicPage) {
    const token = localStorage.getItem('token')
    if (!token) {
      // Redirect to appropriate login page
      if (to.path.startsWith('/patient')) {
        return next('/patient/login')
      } else if (to.path.startsWith('/doctor')) {
        return next('/doctor/login')
      } else if (to.path.startsWith('/lab')) {
        return next('/lab/login')
      } else if (to.path.startsWith('/pharmacy')) {
        return next('/pharmacy/login')
      } else if (to.path.startsWith('/admin')) {
        return next('/admin/login')
      }
    }
  }

  next()
})

export default router
