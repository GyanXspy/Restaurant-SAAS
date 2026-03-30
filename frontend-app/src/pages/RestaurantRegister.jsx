import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { RestaurantService } from '../api/services';
import { Store, MapPin, Info } from 'lucide-react';

export default function RestaurantRegister() {
  const [formData, setFormData] = useState({
    name: '',
    cuisine: '',
    street: '',
    city: '',
    zipCode: '',
    country: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    if (!formData.name || !formData.cuisine || !formData.street || !formData.city) {
      setError('Please fill in all required fields');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await RestaurantService.register(formData);
      setSuccess(true);
      setTimeout(() => {
        navigate('/');
      }, 3000);
    } catch (err) {
      console.error(err);
      setError('Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="flex justify-center items-center" style={{ minHeight: '70vh', padding: '2rem 1rem' }}>
        <div className="card text-center" style={{ width: '100%', maxWidth: '440px', display: 'flex', flexDirection: 'column', gap: '1rem', alignItems: 'center' }}>
          <div style={{ width: '64px', height: '64px', backgroundColor: '#d1fae5', color: 'var(--success)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1rem' }}>
            <Store size={32} />
          </div>
          <h2>Registration Successful!</h2>
          <p className="text-muted">Your restaurant has been successfully registered. You will be redirected to the homepage shortly.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex justify-center items-center" style={{ minHeight: '80vh', padding: '2rem 1rem' }}>
      <div className="card" style={{ width: '100%', maxWidth: '600px', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
          <div style={{ width: '64px', height: '64px', backgroundColor: 'var(--accent-light)', color: 'var(--accent-primary)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Store size={32} />
          </div>
        </div>
        
        <h2 className="text-center">Partner With eats</h2>
        <p className="text-center text-muted" style={{ marginBottom: '1.5rem' }}>Register your restaurant and reach more customers</p>
        
        {error && (
          <div style={{ padding: '0.75rem', backgroundColor: '#fef2f2', border: '1px solid #fca5a5', color: 'var(--danger)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
            <Info size={16} /> {error}
          </div>
        )}

        <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <h3 style={{ fontSize: '1.125rem', borderBottom: '1px solid var(--surface-border)', paddingBottom: '0.5rem', marginTop: '0.5rem' }}>Restaurant Details</h3>
          
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem' }}>
            <div className="input-group" style={{ marginBottom: 0, flex: '1 1 200px' }}>
              <label className="input-label">Restaurant Name *</label>
              <input 
                type="text" 
                name="name"
                value={formData.name}
                onChange={handleChange}
                className="input-field" 
                placeholder="The Golden Spoon"
                required
              />
            </div>
            <div className="input-group" style={{ marginBottom: 0, flex: '1 1 200px' }}>
              <label className="input-label">Cuisine Type *</label>
              <input 
                type="text" 
                name="cuisine"
                value={formData.cuisine}
                onChange={handleChange}
                className="input-field" 
                placeholder="Italian, Fast Food"
                required
              />
            </div>
          </div>

          <h3 style={{ fontSize: '1.125rem', borderBottom: '1px solid var(--surface-border)', paddingBottom: '0.5rem', marginTop: '1rem' }}>Location</h3>

          <div className="input-group" style={{ marginBottom: 0 }}>
            <label className="input-label">Street Address *</label>
            <div style={{ position: 'relative' }}>
              <div style={{ position: 'absolute', top: '50%', left: '12px', transform: 'translateY(-50%)', display: 'flex', alignItems: 'center', pointerEvents: 'none' }}>
                <MapPin className="text-muted" size={16} />
              </div>
              <input 
                type="text" 
                name="street"
                value={formData.street}
                onChange={handleChange}
                className="input-field" 
                style={{ paddingLeft: '2.25rem' }}
                placeholder="123 Food Street"
                required
              />
            </div>
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem' }}>
            <div className="input-group" style={{ marginBottom: 0, flex: '1 1 120px' }}>
              <label className="input-label">City *</label>
              <input 
                type="text" 
                name="city"
                value={formData.city}
                onChange={handleChange}
                className="input-field" 
                placeholder="New York"
                required
              />
            </div>
            <div className="input-group" style={{ marginBottom: 0, flex: '1 1 120px' }}>
              <label className="input-label">Zip Code</label>
              <input 
                type="text" 
                name="zipCode"
                value={formData.zipCode}
                onChange={handleChange}
                className="input-field" 
                placeholder="10001"
              />
            </div>
            <div className="input-group" style={{ marginBottom: 0, flex: '1 1 120px' }}>
              <label className="input-label">Country</label>
              <input 
                type="text" 
                name="country"
                value={formData.country}
                onChange={handleChange}
                className="input-field" 
                placeholder="USA"
              />
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '1rem' }}
          >
            {loading ? 'Registering...' : 'Register Restaurant'}
          </button>
        </form>

        <div className="text-center text-sm mt-4">
          <Link to="/" style={{ fontWeight: '600', color: 'var(--accent-primary)', textDecoration: 'none' }}>
            Return to Home
          </Link>
        </div>
      </div>
    </div>
  );
}
