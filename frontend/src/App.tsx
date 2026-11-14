import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout'
import { AuthLayout } from './components/layout/AuthLayout'
import { ProtectedRoute, PublicOnlyRoute } from './components/layout/RouteGuards'
import { ScrollToTop } from './components/layout/ScrollToTop'
import { ToastProvider } from './components/ui/Toast'
import { BackupsPage } from './pages/BackupsPage'
import { DashboardPage } from './pages/DashboardPage'
import { FilesPage } from './pages/FilesPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { ProfilePage } from './pages/ProfilePage'
import { RegisterPage } from './pages/RegisterPage'
import { RestorePage } from './pages/RestorePage'
import { SchedulesPage } from './pages/SchedulesPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ScrollToTop />
        <ToastProvider>
          <Routes>
            <Route path="/" element={<HomePage />} />

            <Route element={<PublicOnlyRoute />}>
              <Route element={<AuthLayout />}>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
              </Route>
            </Route>

            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/files" element={<FilesPage />} />
                <Route path="/backups" element={<BackupsPage />} />
                <Route path="/restore" element={<RestorePage />} />
                <Route path="/schedules" element={<SchedulesPage />} />
                <Route path="/notifications" element={<NotificationsPage />} />
                <Route path="/profile" element={<ProfilePage />} />
              </Route>
            </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </ToastProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
