/** @type {import('tailwindcss').Config} */
// Enterprise design system — SmartProcure
export default {
  darkMode: ['class'],
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        /* ── Semantic aliases mapped to CSS vars ── */
        border:     'hsl(var(--border))',
        workspace:  'hsl(var(--bg-workspace))',
        card:       { DEFAULT: 'hsl(var(--bg-card))', foreground: 'hsl(var(--text-primary))' },
        muted:      { DEFAULT: 'hsl(var(--bg-muted))', foreground: 'hsl(var(--text-muted))' },
        primary:    { DEFAULT: 'hsl(var(--color-primary))', foreground: 'hsl(var(--color-primary-fg))' },
        success:    'hsl(var(--color-success))',
        warning:    'hsl(var(--color-warning))',
        danger:     'hsl(var(--color-danger))',
        info:       'hsl(var(--color-info))',
        sidebar: {
          bg:         'hsl(var(--sidebar-bg))',
          border:     'hsl(var(--sidebar-border))',
          fg:         'hsl(var(--sidebar-fg))',
          'fg-active':'hsl(var(--sidebar-fg-active))',
          accent:     'hsl(var(--sidebar-accent))',
        },
      },
      textColor: {
        primary:   'hsl(var(--text-primary))',
        secondary: 'hsl(var(--text-secondary))',
        muted:     'hsl(var(--text-muted))',
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
        xl: 'var(--radius-xl)',
      },
      boxShadow: {
        xs: 'var(--shadow-xs)',
        sm: 'var(--shadow-sm)',
        md: 'var(--shadow-md)',
        lg: 'var(--shadow-lg)',
        card: '0 1px 3px 0 rgb(0 0 0 / 0.08), 0 1px 2px -1px rgb(0 0 0 / 0.08)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      fontSize: {
        '2xs': ['0.625rem', { lineHeight: '1rem' }],
        xs:    ['0.75rem',  { lineHeight: '1rem' }],
        sm:    ['0.875rem', { lineHeight: '1.25rem' }],
        base:  ['1rem',     { lineHeight: '1.5rem' }],
        lg:    ['1.125rem', { lineHeight: '1.75rem' }],
        xl:    ['1.25rem',  { lineHeight: '1.75rem' }],
        '2xl': ['1.5rem',   { lineHeight: '2rem' }],
        '3xl': ['1.875rem', { lineHeight: '2.25rem' }],
      },
      spacing: {
        sidebar: 'var(--sidebar-width)',
        'sidebar-collapsed': 'var(--sidebar-collapsed)',
        navbar: '56px',
      },
      transitionTimingFunction: {
        'ease-spring': 'cubic-bezier(0.34, 1.56, 0.64, 1)',
      },
      keyframes: {
        'fade-in':    { from: { opacity: '0' },                    to: { opacity: '1' } },
        'slide-up':   { from: { opacity: '0', transform: 'translateY(8px)' }, to: { opacity: '1', transform: 'translateY(0)' } },
        'slide-in-left': { from: { transform: 'translateX(-100%)' }, to: { transform: 'translateX(0)' } },
        'pulse-soft': { '0%,100%': { opacity: '1' },              '50%': { opacity: '0.5' } },
        shimmer:      { from: { backgroundPosition: '-200% 0' },  to: { backgroundPosition: '200% 0' } },
      },
      animation: {
        'fade-in':    'fade-in 0.2s ease-out',
        'slide-up':   'slide-up 0.25s ease-out',
        'slide-in-left': 'slide-in-left 0.25s ease-out',
        shimmer:      'shimmer 1.6s linear infinite',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
}
