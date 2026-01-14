import {createContext, ReactNode, useContext, useEffect, useRef, useState,} from "react";
import {keycloakInstance} from "@/auth/keycloak";
import {ErrorAlertComponent} from "@/components/error-alert-component";
import {Loading} from "@/pages/loading";

interface LoginOptions {
  idpHint?: string;
  scope?: string;
  redirectUri?: string;
  // Add other options you might need
}

type KeycloakUser = {
  id?: string;
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
};

interface KeycloakContextType {
  isInitialized: boolean;
  isLoading: boolean;
  user: KeycloakUser | null;
  authenticated?: boolean;
  login: (options?: LoginOptions) => Promise<void>;
  logout: () => Promise<void>;
  token?: string;
  tokenParsed?: Record<string, string>;
  refreshToken?: string;
  updateToken: (minValidity: number) => Promise<boolean>;
  error: Error | null;
  refreshTokenWarning: boolean;
}

const KeycloakContext = createContext<KeycloakContextType | undefined>(
  undefined
);

export function KeycloakProvider({ children }: { children: ReactNode }) {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<KeycloakUser | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [refreshTokenWarning, setRefreshTokenWarning] = useState(false);
  const refreshIntervalRef = useRef<number | undefined>(undefined);


  const login = async (options?: LoginOptions) => {
    try {
      await keycloakInstance.login({
        idpHint: options?.idpHint,
        scope: options?.scope,
        redirectUri: options?.redirectUri || window.location.origin,
      });
    } catch (error) {
      console.error("Login failed:", error);
      throw error;
    }
  };

  const refreshToken = async() => {
    try {
      const refreshed = await keycloakInstance.updateToken(60);
      if (refreshed) {
        setRefreshTokenWarning(false);
      }
    } catch (error) {
      console.error("Failed to refresh token", error);
      setRefreshTokenWarning(true);
    }
  }

  const setupTokenRefresh = () => {
    if (refreshIntervalRef.current) {
      window.clearInterval(refreshIntervalRef.current);
    }

    refreshIntervalRef.current = window.setInterval(() => {
      refreshToken();
    }, 30000);
  };

  useEffect(() => {
    return () => {
      if (refreshIntervalRef.current) {
        window.clearInterval(refreshIntervalRef.current);
      }
    }
  }, []);

  useEffect(() => {
    const initKeycloak = async () => {
      try {
        const authenticated = await keycloakInstance.init({
          onLoad: "check-sso",
          checkLoginIframe: false,
          enableLogging: true,
          useNonce: false,
          checkLoginIframeInterval: 600,
          silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        });

        if (authenticated) {
          const userProfile = await keycloakInstance.loadUserProfile();
          const roles = keycloakInstance.realmAccess?.roles || [];
          setUser({
            id: keycloakInstance.subject,
            username: userProfile.username,
            email: userProfile.email,
            firstName: userProfile.firstName,
            lastName: userProfile.lastName,
            roles,
          });

          setupTokenRefresh();

          keycloakInstance.onTokenExpired = () => {
            console.log("Token expired, refreshing...");
            refreshToken();
          }

          keycloakInstance.onAuthRefreshError = () => {
            console.error("Auth refresh error");
            setRefreshTokenWarning(true);
          }

        }
        setIsInitialized(true);
      } catch (error) {
        console.error("Keycloak init error:", error);
        setError(error instanceof Error ? error : new Error(String(error)));
        setIsInitialized(true); // Still set initialized to true so we render something
      } finally {
        setIsLoading(false);
      }
    };

    initKeycloak();
  }, []);

  const wrappedUpdateToken = async (minValidity: number) => {
    try {
      return await keycloakInstance.updateToken(minValidity);
    } catch (error) {
      console.error("Failed to update token:", error);
      setRefreshTokenWarning(true);
      throw error;
    }
  };

  const value: KeycloakContextType = {
    isInitialized,
    isLoading,
    user,
    authenticated: keycloakInstance.authenticated,
    login,
    logout: async () => {
      if (refreshIntervalRef.current) {
        window.clearInterval(refreshIntervalRef.current);
        refreshIntervalRef.current = undefined;
      }
      return keycloakInstance.logout();
    },
    token: keycloakInstance.token || "",
    tokenParsed: keycloakInstance.tokenParsed,
    refreshToken: keycloakInstance.refreshToken,
    updateToken: wrappedUpdateToken,
    error, // Add error to the context value
    refreshTokenWarning,
  };

  const RefreshTokenWarning = () => {
    return (
      <div className="fixed top-0 left-0 right-0 bg-red-600 text-white p-3 text-center z-50">
        Your session is about to expire. Please save your work and refresh the page to continue.
      </div>
    )
  };

  // Show error UI when authentication fails
  if (error) {
    return (
      <ErrorAlertComponent
        error="We couldn't connect to the authentication service. Administrator: Is the `web-origins` client scope set?"
        showButtons={true}
      />
    );
  }

  // Show loading state when initializing
  if (!isInitialized) {
    return <Loading cardTitle="Initialising Authentication" />;
  }

  // Normal rendering when everything is okay
  return (
    <KeycloakContext.Provider value={value}>
      {children}
    </KeycloakContext.Provider>
  );
}

export const useKeycloak = () => {
  const context = useContext(KeycloakContext);
  if (context === undefined) {
    throw new Error("useKeycloak must be used within a KeycloakProvider");
  }
  return context;
};
