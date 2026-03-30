import { Link } from 'react-router-dom';
import { Star, Clock } from 'lucide-react';

export default function RestaurantCard({ restaurant }) {
  // Use food images with warm tones for contrast against pearl green
  let imageUrl = `https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=600&q=80`;
  if (restaurant.cuisine?.includes('Pizza')) imageUrl = 'https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=600&q=80';
  if (restaurant.cuisine?.includes('Healthy')) imageUrl = 'https://images.unsplash.com/photo-1498837167922-41cfa6f31bf0?auto=format&fit=crop&w=600&q=80';
  if (restaurant.cuisine?.includes('Biryani')) imageUrl = 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=600&q=80';

  const rating = restaurant.rating || (Math.random() * (5 - 3.8) + 3.8).toFixed(1);
  const deliveryTime = restaurant.deliveryTime || '30-45 min';
  const cost = restaurant.costForTwo || 40;

  return (
    <Link to={`/restaurant/${restaurant.id}`} style={{ textDecoration: 'none' }}>
      <div className="card p-0 transition-transform" style={{ cursor: 'pointer', border: '1px solid var(--surface-border)' }}>
        <div style={{ height: '200px', borderRadius: 'var(--radius-lg) var(--radius-lg) 0 0', position: 'relative', overflow: 'hidden' }}>
          <img 
            src={imageUrl} 
            alt={restaurant.name} 
            style={{ width: '100%', height: '100%', objectFit: 'cover', transition: 'transform 0.5s ease' }}
            onMouseOver={e => e.currentTarget.style.transform = 'scale(1.05)'}
            onMouseOut={e => e.currentTarget.style.transform = 'scale(1)'}
          />
          {restaurant.active ? null : (
            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(255,255,255,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(2px)' }}>
              <span style={{ backgroundColor: 'var(--danger)', color: 'white', padding: '4px 12px', borderRadius: '4px', fontWeight: 'bold', fontSize: '0.875rem', letterSpacing: '0.1em', textTransform: 'uppercase' }}>Closed</span>
            </div>
          )}
          
          <div style={{ position: 'absolute', top: '0.75rem', left: '0.75rem', backgroundColor: 'white', color: 'var(--text-primary)', padding: '2px 8px', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 'bold', boxShadow: 'var(--shadow-sm)' }}>
            Promoted
          </div>
          
          <div style={{ position: 'absolute', bottom: '1rem', left: 0, backgroundColor: 'var(--accent-primary)', color: 'white', padding: '4px 12px', borderRadius: '0 4px 4px 0', fontSize: '0.75rem', fontWeight: 'bold', boxShadow: 'var(--shadow-sm)' }}>
            60% OFF up to $5
          </div>
          
          <div style={{ position: 'absolute', bottom: '1rem', right: '0.75rem', backgroundColor: 'rgba(255,255,255,0.95)', color: 'var(--text-primary)', padding: '4px 8px', borderRadius: '4px', backdropFilter: 'blur(4px)', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.75rem', fontWeight: '600', boxShadow: 'var(--shadow-sm)' }}>
            <Clock size={12} style={{ color: 'var(--accent-primary)' }} /> {deliveryTime}
          </div>
        </div>
        
        <div style={{ padding: '1rem 1.25rem', backgroundColor: 'var(--bg-primary)', borderRadius: '0 0 var(--radius-lg) var(--radius-lg)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.25rem' }}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: '700', margin: 0, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '80%' }}>{restaurant.name}</h3>
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', backgroundColor: '#24963f', color: 'white', padding: '2px 6px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 'bold', boxShadow: '0 2px 4px rgba(36, 150, 63, 0.2)' }}>
              <span>{rating}</span>
              <Star size={10} fill="white" strokeWidth={1} />
            </div>
          </div>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.75rem' }}>
            <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '60%' }}>{restaurant.cuisine || 'Various'}</span>
            <span>${cost} for two</span>
          </div>
          
          <div style={{ borderTop: '1px solid var(--surface-border)', paddingTop: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
             <img src="https://b.zmtcdn.com/data/o2_assets/4bf016f32f05d26242cea342f30d47a31595763089.png" alt="safe" style={{ width: '18px', height: '18px' }} />
             <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Follows all Max Safety measures</span>
          </div>
        </div>
      </div>
    </Link>
  );
}
