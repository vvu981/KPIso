/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['selector', '[data-theme="dark"]'],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      /* ─── COLORS — Sistema de colores profesional ────────── */
      colors: {
        /* Surfaces */
        'bg-base': 'var(--bg-base)',
        'bg-surface': 'var(--bg-surface)',
        'bg-surface-dark': 'var(--bg-surface-dark)',
        'bg-elevated': 'var(--bg-elevated)',
        'bg-elevated-hover': 'var(--bg-elevated-hover)',
        'bg-overlay': 'var(--bg-overlay)',

        /* Borders */
        'border-subtle': 'var(--border-subtle)',
        'border-default': 'var(--border-default)',
        'border-strong': 'var(--border-strong)',
        'border-accent': 'var(--border-accent)',

        /* Text */
        'text-primary': 'var(--text-primary)',
        'text-secondary': 'var(--text-secondary)',
        'text-tertiary': 'var(--text-tertiary)',
        'text-disabled': 'var(--text-disabled)',
        'text-inverse': 'var(--text-inverse)',

        /* Accents */
        'accent': 'var(--accent)',
        'accent-hover': 'var(--accent-hover)',
        'accent-active': 'var(--accent-active)',
        'accent-light': 'var(--accent-light)',
        'accent-lighter': 'var(--accent-lighter)',
        'accent-ultra-light': 'var(--accent-ultra-light)',

        /* Secondary */
        'cyan': 'var(--cyan)',
        'cyan-hover': 'var(--cyan-hover)',
        'cyan-light': 'var(--cyan-light)',

        /* Semantic */
        'success': 'var(--success)',
        'success-light': 'var(--success-light)',
        'danger': 'var(--danger)',
        'danger-light': 'var(--danger-light)',
        'warning': 'var(--warning)',
        'warning-light': 'var(--warning-light)',
        'info': 'var(--info)',
        'info-light': 'var(--info-light)',
      },

      /* ─── TYPOGRAPHY ─────────────────────────────────────── */
      fontFamily: {
        sans: "var(--font-sans)",
        mono: "var(--font-mono)",
        display: "var(--font-display)",
      },

      fontSize: {
        'xs': 'var(--text-xs)',
        'sm': 'var(--text-sm)',
        'base': 'var(--text-base)',
        'lg': 'var(--text-lg)',
        'xl': 'var(--text-xl)',
        '2xl': 'var(--text-2xl)',
        '3xl': 'var(--text-3xl)',
        '4xl': 'var(--text-4xl)',
        '5xl': 'var(--text-5xl)',
      },

      fontWeight: {
        'light': 'var(--font-light)',
        'normal': 'var(--font-normal)',
        'medium': 'var(--font-medium)',
        'semibold': 'var(--font-semibold)',
        'bold': 'var(--font-bold)',
        'extrabold': 'var(--font-extrabold)',
        'black': 'var(--font-black)',
      },

      lineHeight: {
        'tight': 'var(--leading-tight)',
        'snug': 'var(--leading-snug)',
        'normal': 'var(--leading-normal)',
        'relaxed': 'var(--leading-relaxed)',
        'loose': 'var(--leading-loose)',
      },

      letterSpacing: {
        'tight': 'var(--tracking-tight)',
        'normal': 'var(--tracking-normal)',
        'wide': 'var(--tracking-wide)',
        'wider': 'var(--tracking-wider)',
      },

      /* ─── SPACING ────────────────────────────────────────── */
      spacing: {
        '0': '0',
        '1': 'var(--space-1)',
        '2': 'var(--space-2)',
        '3': 'var(--space-3)',
        '4': 'var(--space-4)',
        '5': 'var(--space-5)',
        '6': 'var(--space-6)',
        '8': 'var(--space-8)',
        '10': 'var(--space-10)',
        '12': 'var(--space-12)',
        '16': 'var(--space-16)',
        '20': 'var(--space-20)',
        '24': 'var(--space-24)',
      },

      /* ─── SIZING ──────────────────────────────────────────── */
      width: {
        'full': 'var(--size-full)',
        'screen': 'var(--size-screen)',
        'min': 'var(--size-min)',
        'max': 'var(--size-max)',
        'fit': 'var(--size-fit)',
      },

      height: {
        'full': 'var(--size-full)',
        'screen': 'var(--size-screen-h)',
        'min': 'var(--size-min)',
        'max': 'var(--size-max)',
        'fit': 'var(--size-fit)',
      },

      /* ─── BORDER RADIUS ───────────────────────────────────── */
      borderRadius: {
        'none': 'var(--radius-none)',
        'xs': 'var(--radius-xs)',
        'sm': 'var(--radius-sm)',
        'md': 'var(--radius-md)',
        'lg': 'var(--radius-lg)',
        'xl': 'var(--radius-xl)',
        '2xl': 'var(--radius-2xl)',
        '3xl': 'var(--radius-3xl)',
        'full': 'var(--radius-full)',
      },

      /* ─── SHADOWS ─────────────────────────────────────────── */
      boxShadow: {
        'none': '0 0 0 transparent',
        'xs': 'var(--shadow-xs)',
        'sm': 'var(--shadow-sm)',
        'md': 'var(--shadow-md)',
        'lg': 'var(--shadow-lg)',
        'xl': 'var(--shadow-xl)',
        '2xl': 'var(--shadow-2xl)',
        'glow': 'var(--shadow-glow)',
        'focus': 'var(--shadow-focus)',
      },

      /* ─── GRADIENTS ───────────────────────────────────────── */
      backgroundImage: {
        'gradient-brand': 'var(--gradient-brand)',
        'gradient-surface': 'var(--gradient-surface)',
        'gradient-card': 'var(--gradient-card)',
        'gradient-button': 'var(--gradient-button)',
      },

      /* ─── TRANSITIONS ──────────────────────────────────────── */
      transitionDuration: {
        'fast': '150ms',
        'base': '200ms',
        'slow': '300ms',
        'slower': '500ms',
      },

      transitionTimingFunction: {
        'spring': 'cubic-bezier(0.34, 1.56, 0.64, 1)',
        'smooth': 'cubic-bezier(0.4, 0, 0.2, 1)',
      },

      /* ─── Z-INDEX ──────────────────────────────────────────── */
      zIndex: {
        'hide': '-1',
        'auto': 'auto',
        'base': '0',
        'dropdown': 'var(--z-dropdown)',
        'sticky': 'var(--z-sticky)',
        'fixed': 'var(--z-fixed)',
        'modal-bg': 'var(--z-modal-bg)',
        'modal': 'var(--z-modal)',
        'popover': 'var(--z-popover)',
        'tooltip': 'var(--z-tooltip)',
        'toast': 'var(--z-toast)',
        'notification': 'var(--z-notification)',
      },

      /* ─── BREAKPOINTS (Mobile-first) ────────────────────── */
      screens: {
        'xs': '0px',
        'sm': '480px',
        'md': '768px',
        'lg': '1024px',
        'xl': '1280px',
        '2xl': '1536px',
      },

      /* ─── ANIMATION ────────────────────────────────────────── */
      animation: {
        'spin': 'spin 0.8s linear infinite',
        'slide-down': 'slideDown 0.2s ease',
        'slide-up': 'slideUp 0.2s ease',
        'slide-in': 'slideIn 0.3s ease',
        'fade-in': 'fadeIn 0.3s ease',
        'scale-in': 'scaleIn 0.25s cubic-bezier(0.34, 1.56, 0.64, 1)',
        'float': 'float 3s ease-in-out infinite',
        'pulse-glow': 'pulse-glow 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },

      keyframes: {
        spin: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
        slideDown: {
          'from': { opacity: '0', transform: 'translateY(-8px)' },
          'to': { opacity: '1', transform: 'translateY(0)' },
        },
        slideUp: {
          'from': { opacity: '0', transform: 'translateY(8px)' },
          'to': { opacity: '1', transform: 'translateY(0)' },
        },
        slideIn: {
          'from': { opacity: '0', transform: 'translateX(-12px)' },
          'to': { opacity: '1', transform: 'translateX(0)' },
        },
        fadeIn: {
          'from': { opacity: '0' },
          'to': { opacity: '1' },
        },
        scaleIn: {
          'from': { opacity: '0', transform: 'scale(0.95)' },
          'to': { opacity: '1', transform: 'scale(1)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-12px)' },
        },
        'pulse-glow': {
          '0%, 100%': { boxShadow: '0 0 0 0 var(--accent-glow)' },
          '50%': { boxShadow: '0 0 0 8px transparent' },
        },
      },

      /* ─── BACKDROP FILTERS ─────────────────────────────────── */
      backdropFilter: {
        'none': 'none',
        'blur': 'var(--glass-blur)',
      },

      /* ─── OPACITY ──────────────────────────────────────────── */
      opacity: {
        '0': '0',
        '5': '0.05',
        '10': '0.1',
        '20': '0.2',
        '30': '0.3',
        '40': '0.4',
        '50': '0.5',
        '60': '0.6',
        '70': '0.7',
        '80': '0.8',
        '90': '0.9',
        '95': '0.95',
        '100': '1',
      },
    },
  },
  plugins: [],
}