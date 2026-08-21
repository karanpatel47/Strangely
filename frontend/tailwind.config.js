/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#0B0E14',
        panel: '#12161F',
        panel2: '#181D28',
        line: '#232936',
        signal: '#5EEAD4',
        signalDim: '#1F3A38',
        warn: '#F59E0B',
        danger: '#F1595C',
        text: '#E7EAF0',
        textDim: '#8A93A6'
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'system-ui', 'sans-serif'],
        body: ['"Inter"', 'system-ui', 'sans-serif']
      }
    }
  },
  plugins: []
}
