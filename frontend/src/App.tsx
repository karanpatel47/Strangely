import { Routes, Route } from 'react-router-dom'
import Home from './pages/Home'
import ChatRoom from './pages/ChatRoom'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/chat" element={<ChatRoom />} />
      <Route path="*" element={<Home />} />
    </Routes>
  )
}
// import MaintenancePage from './components/MaintenancePage'

// export default function App() {
//   return <MaintenancePage />
// }
