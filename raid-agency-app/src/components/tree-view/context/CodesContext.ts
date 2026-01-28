import { createContext, useContext} from 'react';
import { CodesContextType  } from './CodesProvider';
// Notification Context
const CodesContext = createContext<CodesContextType | undefined>(undefined);

export const useCodesContext = (): CodesContextType => {
  const context = useContext(CodesContext);
  if (!context) {
    throw new Error('useCodesContext must be used within CodesProvider');
  }
  return context;
};
export { CodesContext };