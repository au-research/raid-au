import { CSSProperties } from 'react';

export const bannerStyles = {
  notificationBanner: {
    fontFamily: "'Inter', system-ui, sans-serif",
    width: '100%',
    borderBottom: '1px solid rgba(0, 0, 0, 0.1)',
  } as CSSProperties,

  notificationBannerInfo: {
    background: '#dbeafe',
    borderLeft: '4px solid #3b82f6',
  } as CSSProperties,

  notificationBannerWarning: {
    background: '#fed7aa',
    borderLeft: '4px solid #f59e0b',
  } as CSSProperties,

  notificationBannerSuccess: {
    background: '#d1fae5',
    borderLeft: '4px solid #10b981',
  } as CSSProperties,

  notificationBannerError: {
    background: '#fee2e2',
    borderLeft: '4px solid #ef4444',
  } as CSSProperties,

  container: {
    maxWidth: '1400px',
    margin: '0 auto',
    padding: '1rem 1.5rem',
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
  } as CSSProperties,

  icon: {
    flexShrink: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '40px',
    height: '40px',
    borderRadius: '8px',
  } as CSSProperties,

  iconInfo: {
    background: '#bfdbfe',
    color: '#1e40af',
  } as CSSProperties,

  iconWarning: {
    background: '#fbbf24',
    color: '#92400e',
  } as CSSProperties,

  iconSuccess: {
    background: '#a7f3d0',
    color: '#047857',
  } as CSSProperties,

  iconError: {
    background: '#fecaca',
    color: '#b91c1c',
  } as CSSProperties,

  content: {
    flex: 1,
    minWidth: 0,
  } as CSSProperties,

  title: {
    fontSize: '0.875rem',
    fontWeight: 600,
    color: '#111827',
    margin: '0 0 0.25rem 0',
  } as CSSProperties,

  message: {
    fontSize: '0.875rem',
    color: '#111827',
    margin: 0,
    lineHeight: 1.5,
  } as CSSProperties,

  actions: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    flexShrink: 0,
  } as CSSProperties,

  button: {
    display: 'inline-flex',
    alignItems: 'center',
    padding: '0.5rem 1rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    textDecoration: 'none',
    border: '1px solid #e5e7eb',
    borderRadius: '6px',
    background: '#ffffff',
    color: '#374151',
    cursor: 'pointer',
    transition: 'all 0.2s',
  } as CSSProperties,

  buttonHover: {
    background: '#f9fafb',
    borderColor: '#d1d5db',
  } as CSSProperties,

  dismiss: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '32px',
    height: '32px',
    background: 'transparent',
    border: 'none',
    borderRadius: '6px',
    color: '#9ca3af',
    cursor: 'pointer',
    transition: 'all 0.2s',
  } as CSSProperties,

  dismissHover: {
    background: '#f3f4f6',
    color: '#374151',
  } as CSSProperties,
};

export const bannerStyleSheet = `
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap');

  .notification-banner {
    font-family: 'Inter', system-ui, sans-serif;
    width: 100%;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    display: flex;
    text-align: center;
    justify-content: center;
  }

  .notification-banner--info {
    background: #dbeafe;
    border-left: 4px solid #3b82f6;
  }

  .notification-banner--warning {
    background: #f5c955;
    border-left: 4px solid #f59e0b;
  }

  .notification-banner--success {
    background: #d1fae5;
    border-left: 4px solid #10b981;
  }

  .notification-banner--error {
    background: #fee2e2;
    border-left: 4px solid #ef4444;
  }

  .notification-banner__container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0.2rem 1.5rem;
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .notification-banner__icon {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: 8px;
  }

  .notification-banner--info .notification-banner__icon {
    background: #bfdbfe;
    color: #1e40af;
  }

  .notification-banner--warning .notification-banner__icon {
    color: #92400e;
  }

  .notification-banner--success .notification-banner__icon {
    background: #a7f3d0;
    color: #047857;
  }

  .notification-banner--error .notification-banner__icon {
    background: #fecaca;
    color: #b91c1c;
  }

  .notification-banner__content {
    flex: 1;
    min-width: 0;
  }

  .notification-banner__title {
    font-size: 0.875rem;
    font-weight: 600;
    color: #111827;
    margin: 0 0 0.25rem 0;
  }

  .notification-banner__message {
    font-size: 0.875rem;
    color: #000000de;
    margin: 0;
    line-height: 1.5;
  }
    .notification-banner__message a {
    color: #2563eb;
    text-decoration: underline;
    font-weight: 500;
    transition: color 0.2s;
  }

  .notification-banner__message a:hover {
    color: #1d4ed8;
  }

  .notification-banner--warning .notification-banner__message a {
    color: #d97706;
  }

  .notification-banner--warning .notification-banner__message a:hover {
    color: #b45309;
  }

  .notification-banner--error .notification-banner__message a {
    color: #dc2626;
  }

  .notification-banner--error .notification-banner__message a:hover {
    color: #b91c1c;
  }

  .notification-banner--success .notification-banner__message a {
    color: #059669;
  }

  .notification-banner--success .notification-banner__message a:hover {
    color: #047857;
  }

  .notification-banner__actions {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-shrink: 0;
  }

  .notification-banner__button {
    display: inline-flex;
    align-items: center;
    padding: 0.1rem 1rem;
    font-size: 0.875rem;
    font-weight: 500;
    text-decoration: none;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    background: #ffffff;
    color: #374151;
    cursor: pointer;
    transition: all 0.2s;
  }

  .notification-banner__button:hover {
    background: #f9fafb;
    border-color: #d1d5db;
  }

  .notification-banner__dismiss {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    background: transparent;
    border: none;
    border-radius: 6px;
    color: #9ca3af;
    cursor: pointer;
    transition: all 0.2s;
  }

  .notification-banner__dismiss:hover {
    background: #f3f4f6;
    color: #374151;
  }

  /* Responsive */
  @media (max-width: 640px) {
    .notification-banner__container {
      flex-wrap: wrap;
      padding: 1rem;
    }

    .notification-banner__actions {
      width: 100%;
      margin-left: 56px;
    }

    .notification-banner__button {
      flex: 1;
    }
  }
`;
