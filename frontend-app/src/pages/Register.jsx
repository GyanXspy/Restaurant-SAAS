import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { UserService } from '../api/services';
import { UserPlus, Mail, User, Info } from 'lucide-react';
import { useStore } from '../store/useStore';

export default function Register() {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const { setUser } = useStore();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    if (!formData.email || !formData.firstName) {
      setError('First Name and Email are required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      const userId = 'u-' + Math.random().toString(36).substring(2, 9);
      
      const profile = {
        firstName: formData.firstName,
        lastName: formData.lastName || formData.firstName // fallback to first name if last name is missing
      };
      
      if (formData.phone && formData.phone.trim() !== '') {
        profile.phone = formData.phone;
      }
      
      const requestData = {
        userId,
        email: formData.email,
        profile
      };

      await UserService.register(requestData);
      
      // Auto login after successful registration
      setUser({
        id: userId,
        name: `${formData.firstName} ${formData.lastName}`.trim(),
        email: formData.email
      });
      
      navigate('/');
      
    } catch (err) {
      console.error(err);
      if (err.response) {
        // The request was made and the server responded with a status code
        setError(`Backend Error: ${err.response.status} - ${typeof err.response.data === 'string' ? err.response.data : JSON.stringify(err.response.data)}`);
      } else if (err.request) {
        // The request was made but no response was received
        setError('Network Error (CORS or server is down). Please check backend console.');
      } else {
        // Something happened in setting up the request
        setError(`Error: ${err.message}`);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex justify-center items-center" style={{ minHeight: '80vh', padding: '2rem 1rem' }}>
      <div className="card" style={{ width: '100%', maxWidth: '440px', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
          <div style={{ width: '64px', height: '64px', backgroundColor: 'var(--accent-light)', color: 'var(--accent-primary)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <UserPlus size={32} />
          </div>
        </div>
        
        <h2 className="text-center">Create an Account</h2>
        <p className="text-center text-muted" style={{ marginBottom: '1.5rem' }}>Join eats and enjoy great food</p>
        
        {error && (
          <div style={{ padding: '0.75rem', backgroundColor: '#fef2f2', border: '1px solid #fca5a5', color: 'var(--danger)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
            <Info size={16} /> {error}
          </div>
        )}

        <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div style={{ display: 'flex', gap: '1rem' }}>
            <div className="input-group" style={{ marginBottom: 0, flex: 1 }}>
              <label className="input-label">First Name *</label>
              <div style={{ position: 'relative' }}>
                <div style={{ position: 'absolute', top: '50%', left: '12px', transform: 'translateY(-50%)', display: 'flex', alignItems: 'center', pointerEvents: 'none' }}>
                  <User className="text-muted" size={16} />
                </div>
                <input 
                  type="text" 
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  className="input-field" 
                  style={{ paddingLeft: '2.25rem' }}
                  placeholder="John"
                  required
                />
              </div>
            </div>
            
            <div className="input-group" style={{ marginBottom: 0, flex: 1 }}>
              <label className="input-label">Last Name *</label>
              <input 
                type="text" 
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                className="input-field" 
                placeholder="Doe"
                required
              />
            </div>
          </div>

          <div className="input-group" style={{ marginBottom: 0 }}>
            <label className="input-label">Email Address *</label>
            <div style={{ position: 'relative' }}>
              <div style={{ position: 'absolute', top: '50%', left: '12px', transform: 'translateY(-50%)', display: 'flex', alignItems: 'center', pointerEvents: 'none' }}>
                <Mail className="text-muted" size={16} />
              </div>
              <input 
                type="email" 
                name="email"
                value={formData.email}
                onChange={handleChange}
                className="input-field" 
                style={{ paddingLeft: '2.25rem' }}
                placeholder="john@example.com"
                required
              />
            </div>
          </div>
          
          <div className="input-group" style={{ marginBottom: 0 }}>
            <label className="input-label">Phone (Optional)</label>
            <input 
              type="tel" 
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              className="input-field" 
              placeholder="+1 (555) 000-0000"
            />
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '0.5rem' }}
          >
            {loading ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <div className="text-center text-sm mt-6">
          <span className="text-muted">Already have an account? </span>
          <Link to="/login" style={{ fontWeight: '600', color: 'var(--accent-primary)', textDecoration: 'none' }}>
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}
