import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { RestaurantService, CartService } from '../api/services';
import { useStore } from '../store/useStore';
import { MapPin, Clock, ArrowLeft, Plus, Star, ChevronRight } from 'lucide-react';

export default function RestaurantDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useStore();
  
  const [restaurant, setRestaurant] = useState(null);
  const [loading, setLoading] = useState(true);
  const [addingItem, setAddingItem] = useState(null);
  const [activeTab, setActiveTab] = useState('Recommended');
  const [cartSummary, setCartSummary] = useState({ count: 0, total: 0 });

  useEffect(() => {
    fetchRestaurantDetails();
    fetchCartSummary();
  }, [id]);

  const fetchCartSummary = async () => {
    try {
      const res = await CartService.getCart(user.id);
      if(res.data && res.data.items) {
        const count = res.data.items.reduce((acc, item) => acc + item.quantity, 0);
        setCartSummary({ count, total: res.data.totalPrice });
      }
    } catch(e) {
      console.error('Failed to fetch cart summary:', e);
      setCartSummary({ count: 0, total: 0 });
    }
  };

  const fetchRestaurantDetails = async () => {
    try {
      setLoading(true);
      const response = await RestaurantService.getRestaurant(id);
      setRestaurant(response.data);
    } catch (err) {
      console.error('Failed to fetch restaurant details:', err);
      setRestaurant(null);
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = async (item) => {
    setAddingItem(item.id);
    try {
      const cartItem = { menuItemId: item.id, name: item.name, price: item.price, quantity: 1 };
      await CartService.addItem(user.id, cartItem);
      fetchCartSummary(); 
    } catch (err) {
      console.error('Failed to add item to cart:', err);
    } finally {
      setTimeout(() => setAddingItem(null), 400); 
    }
  };

  if (loading) {
    return (
      <div className="container py-16 text-center mt-12 bg-bg-primary">
        <div className="skeleton rounded-xl mb-8" style={{ height: '350px' }}></div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
           <div className="md:col-span-2 flex flex-col gap-4">
             {[1,2,3,4].map(i => <div key={i} className="skeleton h-32 w-full rounded-lg"></div>)}
           </div>
        </div>
      </div>
    );
  }

  if (!restaurant) return <div className="container text-center py-16 mt-12 text-text-muted">Restaurant not found.</div>;

  const tabs = ['Recommended', 'Mains', 'Beverages', 'Desserts'];

  return (
    <div className="bg-bg-primary pb-32 animate-fade-in relative min-h-screen">
      <div className="container pt-24 pb-6">
        <button onClick={() => navigate(-1)} className="flex items-center gap-1 text-text-secondary hover:text-accent-primary mb-4 font-medium transition-colors" style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, fontFamily: 'Inter' }}>
          <ArrowLeft size={16} /> Back to restaurants
        </button>
        
        <div className="flex flex-col md:flex-row gap-4 mb-8">
          <div className="w-full md:w-2/3 rounded-2xl overflow-hidden relative" style={{ height: '320px', border: '1px solid var(--surface-border)' }}>
            <img 
              src="https://images.unsplash.com/photo-1498837167922-41cfa6f31bf0?auto=format&fit=crop&w=1200&q=80" 
              alt={restaurant.name} 
              className="w-full h-full object-cover transition-transform duration-700 hover:scale-105"
            />
             {restaurant.active ? null : (
              <div className="absolute inset-0 bg-white bg-opacity-70 flex items-center justify-center backdrop-blur-sm" style={{ backdropFilter: 'blur(4px)', backgroundColor: 'rgba(255,255,255,0.7)' }}>
                <span className="bg-danger text-white px-4 py-1.5 rounded-md font-bold text-sm tracking-widest uppercase shadow-sm">Currently Closed</span>
              </div>
            )}
          </div>
          
          <div className="w-full md:w-1/3 flex flex-row md:flex-col gap-4">
             <div className="flex-1 rounded-2xl overflow-hidden" style={{ border: '1px solid var(--surface-border)', height: window.innerWidth < 768 ? '140px' : 'calc(50% - 0.5rem)' }}>
                <img src="https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80" className="w-full h-full object-cover hover:scale-105 transition-transform" />
             </div>
             <div className="flex-1 rounded-2xl overflow-hidden" style={{ border: '1px solid var(--surface-border)', height: window.innerWidth < 768 ? '140px' : 'calc(50% - 0.5rem)' }}>
                <img src="https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=600&q=80" className="w-full h-full object-cover hover:scale-105 transition-transform" />
             </div>
          </div>
        </div>

        <div className="flex justify-between items-start mb-6 pb-6" style={{ borderBottom: '1px solid var(--surface-border)' }}>
          <div>
            <h1 className="text-4xl font-bold mb-2 text-text-primary tracking-tight">{restaurant.name}</h1>
            <p className="text-lg text-text-secondary mb-1">{restaurant.cuisine}</p>
            <p className="text-text-muted flex items-center gap-1.5 text-sm"><MapPin size={16}/> {restaurant.address}</p>
            <div className="mt-4 flex items-center gap-4 text-sm text-text-secondary">
               <span className="flex items-center gap-1 font-medium"><Clock size={16} className="text-accent-primary" /> {restaurant.deliveryTime}</span>
               <span className="text-text-muted">•</span>
               <span className="font-medium">${restaurant.costForTwo} for two</span>
            </div>
          </div>
          
          <div className="flex flex-col gap-1 shadow-sm rounded-xl p-3 border border-surface-border text-center bg-white cursor-pointer hover:bg-bg-tertiary transition-colors">
            <div className="flex items-center justify-center font-bold text-xl text-white rounded-lg px-2 py-1 gap-1" style={{ backgroundColor: '#24963f' }}>
              {restaurant.rating} <Star size={14} fill="currentColor" strokeWidth={1} />
            </div>
            <div className="text-xs text-text-muted font-medium border-b border-dashed border-surface-border pb-1 mt-1">Delivery Rating</div>
            <div className="text-xs font-bold text-text-secondary mt-1 tracking-wide">{restaurant.reviews} Reviews</div>
          </div>
        </div>

        {/* Menu Tabs & Listing */}
        <div className="grid md:grid-cols-4 gap-8">
          <div className="md:col-span-1 hidden md:block">
            <div style={{ position: 'sticky', top: '7rem' }}>
              <h3 className="text-xl font-bold mb-4 font-poppins">Menu</h3>
              <div className="flex flex-col border-r border-surface-border">
                {tabs.map(tab => (
                  <button 
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={`text-left py-4 px-4 font-medium transition-colors ${activeTab === tab ? 'text-accent-primary' : 'text-text-secondary hover:text-text-primary'}`}
                    style={{ 
                      background: activeTab === tab ? 'linear-gradient(90deg, transparent 0%, var(--accent-light) 100%)' : 'transparent', 
                      border: 'none', 
                      borderRight: activeTab === tab ? '3px solid var(--accent-primary)' : '3px solid transparent', 
                      cursor: 'pointer',
                      fontSize: '1rem',
                      fontFamily: 'Inter'
                    }}
                  >
                    {tab}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="md:col-span-3">
             <div className="flex md:hidden overflow-x-auto gap-3 mb-6 pb-2" style={{ scrollbarWidth: 'none' }}>
                {tabs.map(tab => (
                  <button 
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={`px-5 py-2.5 rounded-full font-medium whitespace-nowrap transition-colors shadow-sm ${activeTab === tab ? 'bg-accent-primary text-white' : 'bg-white border text-text-secondary'}`}
                    style={{ border: activeTab === tab ? '1px solid var(--accent-primary)' : '1px solid var(--surface-border)', cursor: 'pointer', fontFamily: 'Inter', fontSize: '0.9rem' }}
                  >
                    {tab}
                  </button>
                ))}
             </div>

             <h2 className="text-2xl font-bold mb-6 pt-2 text-text-primary border-b border-surface-border pb-4">{activeTab}</h2>
             
             {restaurant.menuItems.filter(item => activeFilterCategory(item, activeTab)).length === 0 ? (
               <div className="text-center py-12 bg-bg-tertiary rounded-xl border border-surface-border">
                 <p className="text-text-secondary font-medium">No items available in this category.</p>
               </div>
             ) : (
               <div className="flex flex-col gap-0">
                 {restaurant.menuItems.filter(item => activeFilterCategory(item, activeTab)).map(item => (
                   <div key={item.id} className="py-6 flex justify-between gap-6 border-b border-surface-border transition-colors hover:bg-bg-tertiary rounded-xl px-4" style={{ borderBottom: '1px solid var(--surface-border)', marginTop: '-1px' }}>
                     <div className="flex-1">
                       <div className="flex items-center gap-2 mb-2">
                          <img src={item.isVeg ? "https://b.zmtcdn.com/data/o2_assets/bbf7d6b38c92abbbc6a090eec95df93a1596788573.png" : "https://b.zmtcdn.com/data/o2_assets/fc2c93fc133be46067b6678ab2edbaba1596788581.png"} alt={item.isVeg ? 'Veg' : 'Non-veg'} style={{ width: '16px', height: '16px' }} />
                          <h3 className="text-lg font-bold text-text-primary m-0">{item.name}</h3>
                       </div>
                       <p className="font-semibold text-text-primary mb-2 text-lg">${item.price.toFixed(2)}</p>
                       <p className="text-text-secondary text-sm leading-relaxed max-w-xl">{item.description}</p>
                     </div>
                     <div className="relative flex flex-col items-center justify-center w-32 h-32 rounded-xl overflow-hidden shadow-sm border border-surface-border" style={{ flexShrink: 0 }}>
                        <img src={`https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=200&q=80`} className="w-full h-full object-cover" />
                        <button 
                          onClick={() => handleAddToCart(item)}
                          disabled={!item.available || addingItem === item.id || !restaurant.active}
                          className="absolute bg-bg-primary text-accent-primary font-bold border border-surface-border rounded-lg shadow-md hover:bg-accent-light transition-transform active:scale-95 flex items-center justify-center"
                          style={{ bottom: '-10px', width: '90px', height: '36px', fontSize: '0.9rem', outline: 'none', cursor: 'pointer', zIndex: 10, letterSpacing: '0.05em' }}
                        >
                          {addingItem === item.id ? (
                             <span style={{ width: '16px', height: '16px', border: '2px solid var(--accent-light)', borderTopColor: 'var(--accent-primary)', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></span>
                          ) : 'ADD'}
                          {!addingItem && <Plus size={14} className="ml-1" strokeWidth={3} />}
                        </button>
                     </div>
                   </div>
                 ))}
               </div>
             )}
          </div>
        </div>
      </div>

      {/* Sticky Cart Summary Pearl Green Highlight */}
      {cartSummary.count > 0 && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-surface-border p-4 shadow-lg z-50 animate-fade-in sm:bottom-6 sm:left-auto sm:right-6 sm:w-80 sm:rounded-2xl sm:border sm:p-0" style={{ boxShadow: '0 10px 40px rgba(0,0,0,0.1)' }}>
          <div className="flex justify-between items-center bg-accent-primary text-white rounded-xl sm:rounded-2xl p-4 cursor-pointer hover:bg-accent-secondary transition-colors" style={{ boxShadow: 'var(--shadow-glow)' }} onClick={() => navigate('/cart')}>
            <div className="flex flex-col">
              <span className="font-semibold text-xs tracking-wider opacity-90">{cartSummary.count} ITEM{cartSummary.count > 1 ? 'S' : ''}</span>
              <span className="font-bold text-lg">${cartSummary.total.toFixed(2)}</span>
            </div>
            <div className="flex items-center gap-2 font-bold font-poppins text-md">
              View Cart <ChevronRight size={18} strokeWidth={3} />
            </div>
          </div>
        </div>
      )}
      
      <style>{`
        @keyframes spin { 100% { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
  
  function activeFilterCategory(item, tab) {
     if(tab === 'Recommended') return item.category === 'Recommended';
     return item.category === tab;
  }
}
