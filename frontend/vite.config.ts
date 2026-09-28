import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    open: false,
    cors: true,
    proxy: {
      "/api": {
        target: process.env.VITE_BACKEND_URL || "http://localhost:8085",
        changeOrigin: true,
        secure: false
      },
      "/actuator": {
        target: process.env.VITE_BACKEND_URL || "http://localhost:8085",
        changeOrigin: true
      },
      "/swagger-ui": {
        target: process.env.VITE_BACKEND_URL || "http://localhost:8085",
        changeOrigin: true
      },
      "/v3/api-docs": {
        target: process.env.VITE_BACKEND_URL || "http://localhost:8085",
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: "dist",
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ["react", "react-dom"],
          icons: ["@phosphor-icons/react"],
          gis: ["leaflet"],
          charts: ["chart.js"]
        }
      }
    }
  }
});
