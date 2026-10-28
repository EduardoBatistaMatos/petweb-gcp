import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import PetList from './pages/PetList.tsx'
import PetCreate from './pages/PetCreate.tsx'
import PetView from './pages/PetView.tsx'
import PetEdit from './pages/PetEdit.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/pets" replace />} />
        <Route path="/pets" element={<PetList />} />
        <Route path="/pets/novo" element={<PetCreate />} />
        <Route path="/pets/:id" element={<PetView />} />
        <Route path="/pets/:id/editar" element={<PetEdit />} />
        <Route path="/app" element={<App />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)
