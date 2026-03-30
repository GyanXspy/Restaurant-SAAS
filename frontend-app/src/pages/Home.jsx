import { useState, useEffect } from 'react';
import { RestaurantService } from '../api/services';
import RestaurantCard from '../components/RestaurantCard';
import { Search, MapPin, Filter } from 'lucide-react';

export default function Home() {
  const [restaurants, setRestaurants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeFilter, setActiveFilter] = useState('All');

  useEffect(() => {
    fetchRestaurants();
  }, []);

  const fetchRestaurants = async () => {
    try {
      setLoading(true);
      const response = await RestaurantService.getRestaurants();
      setRestaurants(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Failed to fetch restaurants:', err);
      setRestaurants([]);
    } finally {
      setLoading(false);
    }
  };

  const categories = [
    { name: 'Healthy', img: 'https://b.zmtcdn.com/data/o2_assets/019409fe8f838312214d9211be010ef31678798444.jpeg' },
    { name: 'Pizza', img: 'https://b.zmtcdn.com/data/o2_assets/d0bd7c9405ac87f6aa65c31fe55800941632716575.png' },
    { name: 'Biryani', img: 'https://b.zmtcdn.com/data/dish_images/d19a31d42d5913ff129cafd7cec772f81639737192.png' },
    { name: 'Cake', img: 'https://b.zmtcdn.com/data/dish_images/d5ab931c8c239271de45e1c159af94311634805744.png' },
    { name: 'Burger', img: 'https://b.zmtcdn.com/data/dish_images/ccb7dc2ba2b054419f805da7f05704471634886169.png' },
    { name: 'Rolls', img: 'https://b.zmtcdn.com/data/dish_images/c2f22c42f7ba90d81440a88449f4e5891634806087.png' }
  ];

  const filters = ['All', 'Rating 4.0+', 'Fast Delivery', 'Pure Veg', 'Offers'];

  const filteredRestaurants = restaurants.filter(r => 
    ((r.name && r.name.toLowerCase().includes(searchTerm.toLowerCase())) || 
    (r.cuisine && r.cuisine.toLowerCase().includes(searchTerm.toLowerCase()))) &&
    (activeFilter === 'All' || 
     (activeFilter === 'Rating 4.0+' && r.rating >= 4.0) ||
     (activeFilter === 'Fast Delivery' && parseInt(r.deliveryTime) <= 30)
    )
  );

  return (
    <div className="pb-16 bg-bg-tertiary">
      {/* Zomato-style Hero Section with warm food imagery & Soft Pearl Green Overlay */}
      <div 
        style={{ 
          height: '450px',
          background: 'linear-gradient(rgba(80, 200, 120, 0.1), rgba(0, 0, 0, 0.7)), url(https://images.unsplash.com/photo-1543352634-99a5d50ae78e?auto=format&fit=crop&w=1920&q=80)',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          marginTop: '4.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}
      >
        <div className="container relative z-10 text-center px-4" style={{ width: '100%', maxWidth: '900px' }}>
          <h1 className="text-white text-5xl font-bold mb-8" style={{ textShadow: '0 2px 10px rgba(0,0,0,0.3)', letterSpacing: '-0.02em' }}>
            Discover the best food & drinks in <span style={{ color: 'var(--accent-primary)' }}>City</span>
          </h1>
          
          <div style={{ display: 'flex', flexDirection: window.innerWidth < 768 ? 'column' : 'row', backgroundColor: 'white', borderRadius: '0.75rem', padding: '0.5rem', boxShadow: '0 10px 25px rgba(0,0,0,0.1)', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', flex: 1, padding: '0 1rem', borderRight: window.innerWidth < 768 ? 'none' : '1px solid var(--surface-border)', borderBottom: window.innerWidth < 768 ? '1px solid var(--surface-border)' : 'none', height: '3.5rem' }}>
              <MapPin className="text-danger mr-2" size={22} style={{ color: '#e23744' }} />
              <input type="text" placeholder="New York City, NY" style={{ width: '100%', border: 'none', outline: 'none', fontSize: '1rem', color: 'var(--text-primary)', fontFamily: 'Inter' }} />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', flex: 2, padding: '0 1rem', height: '3.5rem' }}>
              <Search className="text-muted mr-3" size={22} />
              <input 
                type="text" 
                placeholder="Search for restaurant, cuisine or a dish" 
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{ width: '100%', border: 'none', outline: 'none', fontSize: '1.05rem', color: 'var(--text-primary)', fontFamily: 'Inter' }}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="container mt-12 bg-bg-primary rounded-xl p-8 shadow-sm" style={{ border: '1px solid var(--surface-border)' }}>
        {/* Categories Section */}
        <div className="mb-10">
          <h2 className="text-2xl font-bold mb-6" style={{ color: 'var(--text-primary)' }}>Inspiration for your first order</h2>
          <div style={{ display: 'flex', gap: '2rem', overflowX: 'auto', paddingBottom: '1rem', scrollbarWidth: 'none', msOverflowStyle: 'none' }}>
            {categories.map((cat, idx) => (
              <div key={idx} onClick={() => setSearchTerm(cat.name)} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.75rem', cursor: 'pointer', minWidth: '120px', transition: 'transform 0.2s ease' }} onMouseOver={e => e.currentTarget.style.transform = 'scale(1.05)'} onMouseOut={e => e.currentTarget.style.transform = 'scale(1)'}>
                <div style={{ width: '140px', height: '140px', borderRadius: '50%', overflow: 'hidden', boxShadow: 'var(--shadow-sm)' }}>
                  <img src={cat.img} alt={cat.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                </div>
                <span style={{ fontWeight: '600', color: 'var(--text-secondary)', fontSize: '1.1rem' }}>{cat.name}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="container mt-10">
        {/* Filters */}
        <div style={{ display: 'flex', gap: '1rem', marginBottom: '2rem', overflowX: 'auto', paddingBottom: '0.5rem', scrollbarWidth: 'none' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 16px', backgroundColor: 'white', border: '1px solid var(--surface-border)', borderRadius: '8px', cursor: 'pointer', boxShadow: '0 1px 3px rgba(0,0,0,0.02)' }}>
            <Filter size={16} className="text-muted" /> <span style={{ fontSize: '0.875rem', fontWeight: '500' }}>Filters</span>
          </div>
          {filters.map(filter => (
            <div 
              key={filter} 
              onClick={() => setActiveFilter(filter)}
              style={{
                padding: '8px 16px',
                borderRadius: '8px',
                cursor: 'pointer',
                fontSize: '0.875rem',
                fontWeight: '500',
                transition: 'all 0.2s ease',
                whiteSpace: 'nowrap',
                backgroundColor: activeFilter === filter ? 'var(--accent-light)' : 'white',
                border: activeFilter === filter ? '1px solid var(--accent-primary)' : '1px solid var(--surface-border)',
                color: activeFilter === filter ? 'var(--accent-primary)' : 'var(--text-secondary)',
                boxShadow: '0 1px 3px rgba(0,0,0,0.02)'
              }}
            >
              {filter}
            </div>
          ))}
        </div>

        {/* Featured Restaurants / Grid */}
        <h2 className="text-3xl font-bold mb-8" style={{ color: 'var(--text-primary)' }}>Delivery Restaurants in City</h2>
        
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {[1, 2, 3, 4, 5, 6].map(i => (
              <div key={i} style={{ height: '340px', backgroundColor: 'white', borderRadius: 'var(--radius-lg)', padding: 0, overflow: 'hidden', border: '1px solid var(--surface-border)' }}>
                <div className="skeleton" style={{ height: '220px', borderRadius: 'var(--radius-lg) var(--radius-lg) 0 0' }}></div>
                <div style={{ padding: '1rem' }}>
                  <div className="flex justify-between w-full mb-2">
                    <div className="skeleton h-6" style={{ width: '60%' }}></div>
                    <div className="skeleton h-6" style={{ width: '15%' }}></div>
                  </div>
                  <div className="skeleton h-4" style={{ width: '80%' }}></div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <>
            {filteredRestaurants.length === 0 ? (
              <div className="text-center py-20 bg-white rounded-2xl border border-surface-border shadow-sm">
                <p className="text-xl text-text-secondary font-medium">No restaurants found matching your criteria.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                {filteredRestaurants.map((restaurant) => (
                  <RestaurantCard key={restaurant.id} restaurant={restaurant} />
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
