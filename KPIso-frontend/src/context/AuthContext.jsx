import { useState, useEffect } from 'react';
import { AuthContext } from './authContextValue.js';

// Proveedor que gestiona los tokens y nombres
export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [userId, setUserId] = useState(localStorage.getItem('userId'));
  const [username, setUsername] = useState(localStorage.getItem('username'));
  const [email, setEmail] = useState(localStorage.getItem('email'));
  const [profilePictureUrl, setProfilePictureUrl] = useState(localStorage.getItem('profilePictureUrl'));
  const [isInitialized, setIsInitialized] = useState(true);

  // Sincronizar el contexto con cambios en localStorage
  useEffect(() => {
    const handleStorageChange = () => {
      setToken(localStorage.getItem('token'));
      setUserId(localStorage.getItem('userId'));
      setUsername(localStorage.getItem('username'));
      setEmail(localStorage.getItem('email'));
      setProfilePictureUrl(localStorage.getItem('profilePictureUrl'));
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  const loginUser = (accessToken, id, name, mail, picUrl) => {
    localStorage.setItem('token', accessToken);
    localStorage.setItem('userId', id);
    localStorage.setItem('username', name);
    localStorage.setItem('email', mail);
    if (picUrl) localStorage.setItem('profilePictureUrl', picUrl);
    setToken(accessToken);
    setUserId(id);
    setUsername(name);
    setEmail(mail);
    setProfilePictureUrl(picUrl || null);
  };

  const logoutUser = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('email');
    localStorage.removeItem('profilePictureUrl');
    setToken(null);
    setUserId(null);
    setUsername(null);
    setEmail(null);
    setProfilePictureUrl(null);
  };

  const updateProfile = (name, mail, picUrl) => {
    localStorage.setItem('username', name);
    localStorage.setItem('email', mail);
    if (picUrl) localStorage.setItem('profilePictureUrl', picUrl);
    else localStorage.removeItem('profilePictureUrl');
    setUsername(name);
    setEmail(mail);
    setProfilePictureUrl(picUrl || null);
  };

  return (
      <AuthContext.Provider value={{ token, userId, username, email, profilePictureUrl, loginUser, logoutUser, updateProfile, isInitialized }}>
        {children}
      </AuthContext.Provider>
  );
};