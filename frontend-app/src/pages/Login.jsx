import { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useStore } from '../store/useStore';
import { UserService } from '../api/services';
import { LogIn, Mail } from 'lucide-react';

export default function Login() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();
  const { setUser } = useStore();

  const from = location.state?.from?.pathname || '/';

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!email) {
      setError('Email is required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const response = await UserService.getAllUsers();
      const users = response.data;
      
      const foundUser = users.find(u => u.email === email);
      
      if (foundUser) {
        setUser({
          id: foundUser.userId,
          name: foundUser.profile?.firstName + ' ' + foundUser.profile?.lastName || foundUser.userId,
          email: foundUser.email
        });
        navigate(from, { replace: true });
      } else {
        setError('No user found with this email. Please register first.');
      }
    } catch (err) {
      console.error(err);
      setError('Failed to connect to authentication service. Try again later.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex justify-center items-center" style={{ minHeight: '70vh', padding: '2rem 1rem' }}>
      <div className="card" style={{ width: '100%', maxWidth: '400px', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
          <div style={{ width: '64px', height: '64px', backgroundColor: 'var(--accent-light)', color: 'var(--accent-primary)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <LogIn size={32} />
          </div>
        </div>
        
        <h2 className="text-center">Welcome Back</h2>
        <p className="text-center text-muted" style={{ marginBottom: '1.5rem' }}>Sign in to your eats account</p>
        
        {error && (
          <div style={{ padding: '0.75rem', backgroundColor: '#fef2f2', border: '1px solid #fca5a5', color: 'var(--danger)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', textAlign: 'center', marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div className="input-group" style={{ marginBottom: 0 }}>
            <label className="input-label">Email Address</label>
            <div style={{ position: 'relative' }}>
              <div style={{ position: 'absolute', top: '50%', left: '12px', transform: 'translateY(-50%)', display: 'flex', alignItems: 'center', pointerEvents: 'none' }}>
                <Mail className="text-muted" size={18} />
              </div>
              <input 
                type="email" 
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="input-field" 
                style={{ paddingLeft: '2.5rem' }}
                placeholder="you@example.com"
                required
              />
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '0.5rem' }}
          >
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <div className="text-center text-sm mt-6">
          <span className="text-muted">Don't have an account? </span>
          <Link to="/register" style={{ fontWeight: '600', color: 'var(--accent-primary)', textDecoration: 'none' }}>
            Register here
          </Link>
        </div>
        
        <div className="text-center text-sm mt-4">
          <Link to="/register-restaurant" style={{ fontWeight: '500', color: 'var(--text-secondary)', textDecoration: 'underline' }}>
            Are you a restaurant owner? Partner with us.
          </Link>
        </div>
      </div>
    </div>
  );
}
