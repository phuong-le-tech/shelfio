import { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { User, LoginCredentials, SignupCredentials, isPremium as checkPremium } from '../types/auth';
import { authApi } from '../services/authApi';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (credentials: LoginCredentials) => Promise<void>;
  signup: (credentials: SignupCredentials) => Promise<void>;
  googleAuth: (credential: string) => Promise<void>;
  logout: () => Promise<void>;
  deleteAccount: () => Promise<void>;
  refreshUser: () => Promise<void>;
  isAdmin: boolean;
  isPremium: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    try {
      const currentUser = await authApi.getCurrentUser();
      setUser(currentUser);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  const login = async (credentials: LoginCredentials) => {
    const response = await authApi.login(credentials);
    setUser(response.user);
  };

  const signup = async (credentials: SignupCredentials) => {
    const response = await authApi.signup(credentials);
    setUser(response.user);
  };

  const googleAuth = async (credential: string) => {
    const response = await authApi.googleAuth(credential);
    setUser(response.user);
  };

  const logout = async () => {
    await authApi.logout();
    setUser(null);
  };

  const deleteAccount = async () => {
    await authApi.deleteAccount();
    setUser(null);
  };

  const refreshUser = useCallback(async () => {
    try {
      const currentUser = await authApi.getCurrentUser();
      setUser(currentUser);
    } catch {
      // Keep existing user state — don't log out on transient errors
    }
  }, []);

  const isAdmin = user?.role === 'ADMIN';
  const isPremium = user ? checkPremium(user.role) : false;

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, googleAuth, logout, deleteAccount, refreshUser, isAdmin, isPremium }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
