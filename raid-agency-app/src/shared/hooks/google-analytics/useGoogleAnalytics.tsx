// hooks/useGoogleAnalytics.tsx
import { useEffect } from 'react';

declare global {
  interface Window {
    gtag?: (...args: unknown[]) => void;
  }
}

export const useGoogleAnalytics = () => {
  useEffect(() => {
    const hostname = window.location.hostname;
    const isProduction = import.meta.env.PROD || hostname.includes('prod');
    const isDemo = hostname.includes('demo');
    
    let gaId: string | null = null;
    
    if (isProduction) {
      gaId = import.meta.env.VITE_GA_MEASUREMENT_ID || null;
    } else if (isDemo) {
      gaId = import.meta.env.VITE_GA_MEASUREMENT_ID_DEMO || null;
    } else {
      gaId = null;
    }

    // Only proceed if we have GA ID and in prod/demo
    if (!((isProduction || isDemo) && gaId)) return;
    // Check if GA already loaded
    if (document.querySelector(`script[src*="gtag/js?id=${gaId}"]`)) {
      return;
    }
    // Create the gtag script tag
    const gtagScript = document.createElement('script');
    gtagScript.async = true;
    gtagScript.src = `https://www.googletagmanager.com/gtag/js?id=${gaId}`;
    document.head.appendChild(gtagScript);

    // Create the inline script
    const inlineScript = document.createElement('script');
    inlineScript.textContent = `
      window.dataLayer = window.dataLayer || [];
      function gtag(){window.dataLayer.push(arguments);}
      window.gtag = gtag;
      gtag('js', new Date());
      gtag('config', '${gaId}');
    `;
    document.head.appendChild(inlineScript);
  }, []); // Run once only

  // Simple track event function
  const trackEvent = (eventName: string, parameters?: Record<string, unknown>) => {
    if (window.gtag) {
      window.gtag('event', eventName, parameters || {});
    } else {
      console.warn('GA: gtag not available for event:', eventName);
    }
  };

  return { trackEvent };
};
// Usage: Call useGoogleAnalytics() in your main App component to initialize GA
