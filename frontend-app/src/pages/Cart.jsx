import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { CartService } from '../api/services';
import { useStore } from '../store/useStore';
import { Trash2, Plus, Minus, ShoppingBag, ArrowRight } from 'lucide-react';

export default function Cart() {
  const { user } = useStore();
  const navigate = useNavigate();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchCart();
  }, []);

  const fetchCart = async () => {
    try {
      setLoading(true);
      const response = await CartService.getCart(user.id);
      setCart(response.data);
    } catch (err) {
      console.error('Failed to fetch cart:', err);
      setCart(null);
    } finally {
      setLoading(false);
    }
  };

  const updateQuantity = async (itemId, newQuantity) => {
    if (newQuantity < 1) return;
    try {
      const updatedItems = cart.items.map(i => i.menuItemId === itemId ? { ...i, quantity: newQuantity } : i);
      const newTotal = updatedItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
      setCart({ ...cart, items: updatedItems, totalPrice: newTotal });
      await CartService.updateItem(user.id, itemId, newQuantity);
    } catch (err) {}
  };

  const removeItem = async (itemId) => {
    try {
      const updatedItems = cart.items.filter(i => i.menuItemId !== itemId);
      const newTotal = updatedItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
      setCart({ ...cart, items: updatedItems, totalPrice: newTotal });
      await CartService.removeItem(user.id, itemId);
    } catch (err) {}
  };

  if (loading) {
    return <div className="container py-16 text-center mt-12 bg-bg-tertiary">Loading...</div>;
  }

  const isEmpty = !cart || !cart.items || cart.items.length === 0;

  return (
    <div className="bg-bg-tertiary min-h-screen pb-16 pt-24">
      <div className="container" style={{ maxWidth: '900px' }}>
        <h1 className="text-3xl font-bold mb-8 text-text-primary">
          Secure Checkout
        </h1>
        
        {isEmpty ? (
          <div className="card text-center" style={{ padding: '4rem 2rem' }}>
            <ShoppingBag size={80} className="mx-auto text-text-muted opacity-20 mb-6" />
            <h2 className="text-2xl mb-4 font-semibold text-text-primary">Your cart is empty</h2>
            <p className="text-text-secondary mb-8 font-medium">Add items from a restaurant to start a new order.</p>
            <Link to="/" className="btn btn-primary" style={{ padding: '0.8rem 2rem' }}>Browse Restaurants</Link>
          </div>
        ) : (
          <div className="grid md:grid-cols-3 gap-8 items-start">
            <div className="md:col-span-2 flex flex-col gap-6">
              <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                 <div className="p-5 border-b border-surface-border bg-bg-primary" style={{ borderBottom: '1px solid var(--surface-border)' }}>
                    <h2 className="text-xl font-bold text-text-primary font-poppins m-0">Order Items</h2>
                 </div>
                 <div className="flex flex-col">
                  {cart.items.map((item, index) => (
                    <div key={item.menuItemId} className="flex flex-col sm:flex-row sm:items-center justify-between p-5 gap-4" style={{ borderBottom: index < cart.items.length - 1 ? '1px solid var(--surface-border)' : 'none' }}>
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                           <img src="https://b.zmtcdn.com/data/o2_assets/bbf7d6b38c92abbbc6a090eec95df93a1596788573.png" alt="Veg" className="w-4 h-4" />
                           <h3 className="text-md font-bold text-text-primary m-0">{item.name}</h3>
                        </div>
                        <p className="text-text-secondary font-medium m-0 text-sm">${item.price.toFixed(2)}</p>
                      </div>
                      
                      <div className="flex items-center justify-between sm:justify-end gap-6 w-full sm:w-auto">
                        <div className="flex items-center gap-3 bg-bg-tertiary rounded-lg px-2 py-1 border border-surface-border">
                          <button onClick={() => updateQuantity(item.menuItemId, item.quantity - 1)} className="p-1 text-accent-primary transition-colors cursor-pointer border-none bg-transparent hover:bg-white rounded font-bold"><Minus size={16} strokeWidth={3} /></button>
                          <span className="w-6 text-center font-bold text-sm text-text-primary">{item.quantity}</span>
                          <button onClick={() => updateQuantity(item.menuItemId, item.quantity + 1)} className="p-1 text-accent-primary transition-colors cursor-pointer border-none bg-transparent hover:bg-white rounded font-bold"><Plus size={16} strokeWidth={3} /></button>
                        </div>
                        
                        <div className="w-20 text-right font-bold text-text-primary">
                          ${(item.price * item.quantity).toFixed(2)}
                        </div>
                        
                        <button onClick={() => removeItem(item.menuItemId)} className="p-2 text-text-muted hover:text-danger hover:bg-danger transition-colors bg-transparent border-none cursor-pointer rounded-full" aria-label="Remove item">
                          <Trash2 size={18} />
                        </button>
                      </div>
                    </div>
                  ))}
                 </div>
              </div>
              
              <div className="card">
                 <h3 className="text-lg font-bold mb-4">Delivery Instructions</h3>
                 <textarea className="input-field" rows="2" placeholder="e.g. Leave at the door, call upon arrival..."></textarea>
              </div>
            </div>

            <div className="md:col-span-1">
              <div className="card" style={{ padding: 0, position: 'sticky', top: '6rem' }}>
                <div className="p-5 border-b border-surface-border" style={{ borderBottom: '1px solid var(--surface-border)' }}>
                    <h3 className="text-lg font-bold m-0 font-poppins">Bill Details</h3>
                </div>
                
                <div className="p-5 flex flex-col gap-3">
                  <div className="flex justify-between text-text-secondary text-sm">
                    <span>Item Total</span>
                    <span className="font-medium text-text-primary">${cart.totalPrice.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-text-secondary text-sm">
                    <span>Delivery Fee</span>
                    <span className="font-medium text-accent-primary">FREE</span>
                  </div>
                  <div className="flex justify-between text-text-secondary text-sm">
                    <span>Taxes & Charges</span>
                    <span className="font-medium text-text-primary">${(cart.totalPrice * 0.1).toFixed(2)}</span>
                  </div>
                  
                  <hr className="my-2 border-surface-border" style={{ borderTop: '1px dashed var(--surface-border)', background: 'transparent' }} />
                  
                  <div className="flex justify-between text-lg font-bold text-text-primary items-center">
                    <span>To Pay</span>
                    <span className="text-xl">${(cart.totalPrice * 1.1).toFixed(2)}</span>
                  </div>
                </div>
                
                <div className="p-5 bg-white border-t border-surface-border mt-auto rounded-b-2xl" style={{ borderTop: '1px solid var(--surface-border)' }}>
                  <Link to="/checkout" className="btn btn-primary w-full text-lg shadow-glow py-3" style={{ width: '100%', padding: '1rem', justifyContent: 'center' }}>
                    Select Address <ArrowRight size={18} className="ml-2" />
                  </Link>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
