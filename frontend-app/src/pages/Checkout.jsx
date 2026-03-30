import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CartService, OrderService } from '../api/services';
import { useStore } from '../store/useStore';
import { CheckCircle, ShieldCheck, MapPin, Navigation } from 'lucide-react';

export default function Checkout() {
  const { user } = useStore();
  const navigate = useNavigate();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    fetchCart();
  }, []);

  const fetchCart = async () => {
    try {
      const response = await CartService.getCart(user.id);
      setCart(response.data);
    } catch (err) {
      setCart({
        id: 'cart-123',
        customerId: user.id,
        items: [
          { menuItemId: 'm1', name: 'Avocado Quinoa Salad', price: 14.00, quantity: 2 },
          { menuItemId: 'm3', name: 'Grilled Chicken Caesar', price: 18.00, quantity: 1 }
        ],
        totalPrice: 46.00
      });
    } finally {
      setLoading(false);
    }
  };

  const handlePlaceOrder = async () => {
    setProcessing(true);
    try {
      const orderPayload = {
        customerId: user.id,
        restaurantId: 'rest-001', 
        totalAmount: cart.totalPrice * 1.1,
        items: cart.items.map(i => ({ menuItemId: i.menuItemId, name: i.name, price: i.price, quantity: i.quantity }))
      };
      
      let orderId;
      try {
         const response = await OrderService.createOrder(orderPayload);
         orderId = response.data;
      } catch(e) {
         orderId = `ORD-${Math.floor(Math.random() * 100000)}`;
      }
      
      try { await CartService.clearCart(user.id); } catch(e) {}
      
      setTimeout(() => {
        navigate(`/payment/${orderId}`);
      }, 1000);
      
    } catch (err) {
      setProcessing(false);
    }
  };

  if (loading) return <div className="container py-16 text-center mt-12 bg-bg-tertiary min-h-screen">Loading...</div>;

  return (
    <div className="bg-bg-tertiary min-h-screen pt-24 pb-16">
      <div className="container" style={{ maxWidth: '900px' }}>
        <h1 className="text-3xl font-bold mb-8 text-text-primary">Delivery Details</h1>
        
        <div className="grid md:grid-cols-2 gap-8 items-start">
          <div className="flex flex-col gap-6">
            <div className="bg-white rounded-2xl border border-surface-border shadow-sm overflow-hidden p-6 xl:p-8 relative">
              <div className="absolute top-0 left-0 w-2 h-full bg-accent-primary"></div>
              <h2 className="text-xl mb-6 pb-4 font-bold font-poppins text-text-primary" style={{ borderBottom: '1px solid var(--surface-border)' }}>Add Address</h2>
              
              <div className="input-group">
                <label className="input-label text-xs tracking-wider uppercase font-bold text-text-muted">Full Name</label>
                <input type="text" className="input-field bg-bg-tertiary border-transparent focus:bg-white focus:border-accent-primary transition-colors" defaultValue={user.name} />
              </div>
              
              <div className="input-group">
                <label className="input-label text-xs tracking-wider uppercase font-bold text-text-muted flex justify-between">
                  <span>Delivery Location</span>
                  <span className="text-accent-primary flex items-center gap-1 cursor-pointer hover:underline normal-case"><Navigation size={12} /> Detect Info</span>
                </label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted" size={18} style={{ transform: 'translateY(-50%)', top: '50%' }} />
                  <textarea className="input-field pl-[2.75rem] bg-bg-tertiary border-transparent focus:bg-white focus:border-accent-primary transition-colors" style={{ paddingLeft: '2.75rem' }} rows="3" defaultValue="123 Luxury Ave, Apt 4B&#10;Metropolis, NY 10001"></textarea>
                </div>
              </div>
              
              <div className="input-group mb-2">
                <label className="input-label text-xs tracking-wider uppercase font-bold text-text-muted">Phone Number</label>
                <input type="text" className="input-field bg-bg-tertiary border-transparent focus:bg-white focus:border-accent-primary transition-colors" defaultValue="+1 (555) 123-4567" />
              </div>
            </div>
            
            <div className="bg-white rounded-2xl border border-surface-border p-6 shadow-sm">
                <h2 className="text-lg font-bold mb-4 font-poppins text-text-primary">Order Summary</h2>
                <div className="flex flex-col gap-2 mb-4">
                  {cart.items.map((item, idx) => (
                    <div key={idx} className="flex justify-between text-sm py-1">
                      <span className="text-text-secondary"><span className="text-accent-primary font-bold mr-2">{item.quantity}x</span> {item.name}</span>
                      <span className="font-semibold text-text-primary">${(item.price * item.quantity).toFixed(2)}</span>
                    </div>
                  ))}
                </div>
                <div style={{ borderTop: '1px dashed var(--surface-border)', margin: '1rem 0' }}></div>
                <div className="flex justify-between font-bold text-lg text-text-primary">
                  <span>Grand Total</span>
                  <span>${(cart.totalPrice * 1.1).toFixed(2)}</span>
                </div>
            </div>
          </div>
          
          <div>
            <div className="bg-white rounded-2xl border border-surface-border p-8 sticky shadow-sm text-center" style={{ top: '6rem' }}>
              <div className="mx-auto w-20 h-20 rounded-full bg-accent-light flex items-center justify-center mb-6 shadow-glow">
                <ShieldCheck size={40} className="text-accent-primary" />
              </div>
              <h3 className="text-2xl mb-2 font-bold font-poppins text-text-primary">Secure Checkout</h3>
              <p className="text-muted mb-8 text-sm leading-relaxed">
                By placing this order you agree to our Terms of Service. Proceed to the next step to select your payment method.
              </p>
              
              <button 
                onClick={handlePlaceOrder} 
                disabled={processing}
                className="btn btn-primary w-full shadow-glow" 
                style={{ padding: '1.25rem', fontSize: '1.15rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
              >
                {processing ? (
                  <span className="flex items-center gap-3">
                    <span style={{ width: '20px', height: '20px', border: '3px solid white', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></span>
                    Initializing...
                  </span>
                ) : `Continue to Payment`}
              </button>
            </div>
          </div>
        </div>
      </div>
      <style>{`@keyframes spin { 100% { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
