import React, { useState, useMemo } from 'react';
import { NotificationContext } from './NotificationsContext';
import { Notification, NotificationContextValue, NotificationProviderProps } from './inferface';

export const NotificationProvider: React.FC<NotificationProviderProps> = ({ children }) => {
  const [notifications, setNotifications] = useState<{ [key: string]: Notification }>({});

  // Calculate total count of all categories across all notifications
  const totalCount = useMemo(() => {
    return Object.values(notifications).reduce((total, notification) => {
      return total + (notification.categories?.length || 0);
    }, 0);
  }, [notifications]);

  // Update notifications (merge with existing)
  const updateNotifications = (updates: { [key: string]: Notification }): void => {
    setNotifications(prev => ({
      ...prev,
      ...updates,
    }));
  };

  // Add a single notification
  const addNotification = (key: string, notification: Notification): void => {
    setNotifications(prev => ({
      ...prev,
      [key]: notification,
    }));
  };

  // Remove a notification by key
  const removeNotification = (key: string): void => {
    setNotifications(prev => {
      const newNotifications = { ...prev };
      delete newNotifications[key];
      return newNotifications;
    });
  };

  // Clear all notifications
  const clearAllNotifications = (): void => {
    setNotifications({});
  };

  const contextValue: NotificationContextValue = {
    totalCount,
    notifications,
    updateNotifications,
    addNotification,
    removeNotification,
    clearAllNotifications,
  };

  return (
    <NotificationContext.Provider value={contextValue}>
      {children}
    </NotificationContext.Provider>
  );
};
