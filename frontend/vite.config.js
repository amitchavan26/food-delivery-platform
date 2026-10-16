import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Forward /api calls to the Spring Boot backend, so the browser sees one origin (no CORS needed in dev).
    proxy: { '/api': 'http://localhost:8080' },
  },
})
