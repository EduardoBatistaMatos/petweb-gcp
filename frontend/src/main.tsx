import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import './index.css'
import PetList from './pages/PetList.tsx'
import PetCreate from './pages/PetCreate.tsx'
import PetView from './pages/PetView.tsx'
import PetEdit from './pages/PetEdit.tsx'
import AppLayout from './components/AppLayout.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/pets" replace />} />
          <Route path="/pets" element={<PetList />} />
          <Route path="/pets/novo" element={<PetCreate />} />
          <Route path="/pets/:id" element={<PetView />} />
          <Route path="/pets/:id/editar" element={<PetEdit />} />
          <Route path="/app" element={<Navigate to="/pets" replace />} />
          <Route path="*" element={<Navigate to="/pets" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)
