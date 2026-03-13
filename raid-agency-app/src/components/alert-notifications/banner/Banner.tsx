import React, { useState, useEffect } from 'react';
import { bannerStyleSheet } from './Banner.styles';

type BannerVariant = 'info' | 'warning' | 'success' | 'error';

interface BannerProps {
  title?: string;
  message?: string | React.ReactNode;
  variant?: BannerVariant;
  dismissible?: boolean;
  onDismiss?: () => void;
  action?: () => void;
  actionLabel?: string;
}

const Banner: React.FC<BannerProps> = ({
  title = "",
  message = "We've made some important updates to improve your experience",
  variant = "warning",
  dismissible = true,
  onDismiss,
  action,
  actionLabel = "Learn More",
}) => {
  const [isVisible, setIsVisible] = useState<boolean>(true);

  useEffect(() => {
    // Inject styles into the document head
    const styleId = 'banner-styles';
    if (!document.getElementById(styleId)) {
      const style = document.createElement('style');
      style.id = styleId;
      style.textContent = bannerStyleSheet;
      document.head.appendChild(style);
    }
  }, []);

  const handleDismiss = () => {
    setIsVisible(false);
    onDismiss?.();
  };

  if (!isVisible) return null;

  const icons: Record<BannerVariant, JSX.Element> = {
    info: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
        <path d="M12 16v-4m0-4h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
      </svg>
    ),
    warning: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
        <path d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    ),
    success: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
        <path d="M9 12l2 2 4-4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    ),
    error: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
        <path d="M15 9l-6 6m0-6l6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
      </svg>
    ),
  };

  return (
    <div 
      className={`notification-banner notification-banner--${variant}`}
      role="banner"
      aria-live="polite"
    >
        <div className="notification-banner__container">
          <div className="notification-banner__icon">
            {icons[variant]}
          </div>

          <div className="notification-banner__content">
            <div className="notification-banner__title">{title}</div>
            <div className="notification-banner__message">{message}</div>
          </div>

          <div className="notification-banner__actions">
            {action && (
              <button 
                className="notification-banner__button"
                onClick={action}
              >
                {actionLabel}
              </button>
            )}
            
            {dismissible && (
              <button 
                className="notification-banner__dismiss"
                onClick={handleDismiss}
                aria-label="Dismiss notification"
              >
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                  <path 
                    d="M6 6l8 8M14 6l-8 8" 
                    stroke="currentColor" 
                    strokeWidth="1.5" 
                    strokeLinecap="round"
                  />
                </svg>
              </button>
            )}
          </div>
        </div>
      </div>
  );
};

export default Banner;