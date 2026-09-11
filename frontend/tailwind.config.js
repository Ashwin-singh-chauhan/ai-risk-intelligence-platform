/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eef4ff",
          100: "#d9e6ff",
          200: "#b9d0ff",
          300: "#8ab0ff",
          400: "#5686ff",
          500: "#2f5eff",
          600: "#1c3ee6",
          700: "#182fb4",
          800: "#182a8f",
          900: "#182872",
          950: "#0f1747",
        },
        risk: {
          low: "#16a34a",
          medium: "#eab308",
          high: "#ea580c",
          critical: "#dc2626",
        },
      },
    },
  },
  plugins: [],
};
