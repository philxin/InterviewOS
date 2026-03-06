export interface User {
  id: string
  name: string
  email: string
  avatar?: string
}

export interface Interview {
  id: string
  title: string
  description: string
  duration: number
  questions: Question[]
}

export interface Question {
  id: string
  content: string
  type: string
  answer?: string
  feedback?: string
}

export interface Training {
  id: string
  title: string
  description: string
  status: 'pending' | 'in_progress' | 'completed'
  progress: number
}

export interface Dashboard {
  totalInterviews: number
  completedTrainings: number
  averageScore: number
  recentActivities: Activity[]
}

export interface Activity {
  id: string
  type: string
  description: string
  timestamp: string
}