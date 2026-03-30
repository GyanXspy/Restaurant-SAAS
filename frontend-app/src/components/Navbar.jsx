import { Link, useNavigate } from 'react-router-dom';
import { useStore } from '../store/useStore';
import { ShoppingCart, LogOut, MapPin, Store } from 'lucide-react';

export default function Navbar() {
  const { user, logout } = useStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="navbar">
      <div className="container nav-container">
        <Link to="/" className="nav-logo">
          <span style={{ color: 'var(--accent-primary)', fontSize: '2.2rem', fontStyle: 'italic', letterSpacing: '-0.05em' }}>eats</span>
        </Link>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: 'var(--bg-primary)', padding: '0.6rem 1rem', borderRadius: 'var(--radius-md)', flex: 1, maxWidth: '300px', marginLeft: '2rem', border: '1px solid var(--surface-border)', boxShadow: 'var(--shadow-sm)' }} className="hidden md-flex">
          <MapPin size={18} className="text-danger" />
          <span className="text-sm font-medium text-text-secondary" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>New York City, NY</span>
        </div>
        
        <div className="nav-links">
          {!user && (
            <Link to="/register-restaurant" className="hidden md-flex items-center gap-2 text-text-secondary hover:text-accent-primary transition-colors font-medium" style={{ marginRight: '1rem', textDecoration: 'none' }}>
              <Store size={18} />
              <span>Partner with us</span>
            </Link>
          )}

          <Link to="/cart" className="flex items-center gap-2 text-text-secondary hover:text-accent-primary transition-colors font-medium" style={{ textDecoration: 'none' }}>
            <ShoppingCart size={22} style={{ color: 'var(--text-primary)' }} />
            <span style={{ display: window.innerWidth > 600 ? 'inline' : 'none' }}>Cart</span>
          </Link>
          
          {user ? (
            <>
              <Link to="/profile" className="flex items-center gap-2 text-text-secondary hover:text-accent-primary transition-colors font-medium" style={{ textDecoration: 'none' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '50%', backgroundColor: 'var(--accent-light)', color: 'var(--accent-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: '1rem' }}>
                  {user.name.charAt(0)}
                </div>
                <span style={{ display: window.innerWidth > 600 ? 'inline' : 'none' }}>{user.name}</span>
              </Link>
              <button onClick={handleLogout} className="btn btn-secondary" style={{ padding: '0.4rem 0.8rem', marginLeft: '0.5rem' }}>
                <LogOut size={16} className="text-danger" />
                <span className="text-danger font-medium text-sm">Logout</span>
              </button>
            </>
          ) : (
            <Link to="/login" className="btn btn-primary" style={{ padding: '0.5rem 1.25rem' }}>
              Log in
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
}
