import { ReactNode } from 'react';

// Interfaces
export interface NotificationCategory {
  titleIcon: ReactNode;
  name: string;
  actions?: ReactNode[];
  button?: ReactNode;
  [key: string]: any; // Allow for future extensions
}

export interface Notification {
  title: string;
  categories: NotificationCategory[];
}

export interface NotificationContextValue {
  totalCount: number;
  notifications: { [key: string]: Notification };
  updateNotifications: (updates: { [key: string]: Notification }) => void;
  addNotification: (key: string, notification: Notification) => void;
  removeNotification: (key: string) => void;
  clearAllNotifications: () => void;
}

export interface NotificationProviderProps {
  children: ReactNode;
}
