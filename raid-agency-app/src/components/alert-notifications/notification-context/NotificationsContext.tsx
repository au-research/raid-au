import { createContext, useContext} from 'react';
import { NotificationContextValue  } from './inferface';
// Notification Context
const NotificationContext = createContext<NotificationContextValue | undefined>(undefined);

export const useNotificationContext = (): NotificationContextValue => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotificationContext must be used within NotificationProvider');
  }
  return context;
};
export { NotificationContext };
