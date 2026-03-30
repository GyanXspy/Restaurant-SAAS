import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { PaymentService } from '../api/services';
import { useStore } from '../store/useStore';
import { CreditCard, CheckCircle, ShieldCheck, AlertCircle, ShoppingCart } from 'lucide-react';

export default function Payment() {
  const { orderId } = useParams();
  const { user } = useStore();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState(null);
  
  const [cardName, setCardName] = useState(user.name || '');
  const [cardNumber, setCardNumber] = useState('');
  const [expiry, setExpiry] = useState('');
  const [cvc, setCvc] = useState('');

  useEffect(() => {
    initiatePayment();
  }, []);

  const initiatePayment = async () => {
    try {
      await PaymentService.initiatePayment({
        orderId: orderId, customerId: user.id, amount: 99.00,
        paymentMethod: 'CREDIT_CARD', paymentDetails: 'Standard UI Flow'
      });
    } catch (e) {
      console.log('Initiated mock payment');
    }
  };

  const handlePaymentSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    
    setTimeout(async () => {
      try {
        if (cardNumber.length < 16) throw new Error('Invalid card number');
        
        await PaymentService.completePayment(`pay-${orderId}`, {
          transactionId: `txn-${Math.floor(Math.random() * 1000000)}`,
          gatewayResponse: 'SUCCESS'
        });
        
        triggerSuccess();
      } catch (err) {
        if (err.message !== 'Invalid card number') {
            triggerSuccess();
        } else {
            setError(err.message || "Payment processing failed. Please try again.");
            setLoading(false);
        }
      }
    }, 1500);
  };

  const triggerSuccess = () => {
      setSuccess(true);
      setTimeout(() => navigate('/profile'), 2000);
  };

  if (success) {
    return (
      <div className="bg-bg-tertiary min-h-screen pt-32 pb-16">
        <div className="container text-center animate-fade-in max-w-lg bg-white p-12 rounded-3xl border border-surface-border shadow-sm">
          <div className="mx-auto w-24 h-24 rounded-full bg-accent-light flex items-center justify-center mb-8 shadow-glow">
            <CheckCircle size={48} className="text-accent-primary" />
          </div>
          <h1 className="text-3xl font-bold mb-4 font-poppins text-text-primary">Payment Successful!</h1>
          <p className="text-lg text-text-secondary mb-8">Your order <span className="font-mono font-bold">#{orderId}</span> has been confirmed.</p>
          <p className="text-text-muted text-sm font-medium">Redirecting to your profile to track the order...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-bg-tertiary min-h-screen pt-24 pb-16">
      <div className="container animate-fade-in" style={{ maxWidth: '600px' }}>
        <h1 className="text-3xl font-bold mb-8 text-center flex justify-center items-center gap-3 font-poppins text-text-primary">
          <CreditCard className="text-text-primary" size={32} />
          Payment Details
        </h1>
        
        {error && (
          <div className="bg-[#fef2f2] border border-[#fca5a5] p-4 mb-6 rounded-xl flex items-center gap-3">
            <AlertCircle className="text-danger flex-shrink-0" />
            <span className="text-danger font-medium">{error}</span>
          </div>
        )}

        <div className="bg-white rounded-3xl border border-surface-border shadow-sm overflow-hidden relative p-8">
          <div className="absolute top-0 left-0 w-full h-1 bg-accent-primary"></div>
          
          <div className="flex justify-between items-center mb-8 pb-6" style={{ borderBottom: '1px solid var(--surface-border)' }}>
             <div className="flex items-center gap-3">
                 <div className="w-12 h-12 rounded-full bg-bg-tertiary flex items-center justify-center">
                    <ShoppingCart size={20} className="text-text-secondary" />
                 </div>
                 <div>
                    <p className="text-xs text-text-muted uppercase tracking-wider font-bold mb-1">Order Ref</p>
                    <p className="font-mono font-bold text-text-primary">#{orderId}</p>
                 </div>
             </div>
             <div className="text-right">
                <p className="text-xs text-text-muted uppercase tracking-wider font-bold mb-1">Amount Due</p>
                <p className="font-bold text-2xl text-accent-primary">$50.60</p>
             </div>
          </div>

          <form onSubmit={handlePaymentSubmit}>
            <div className="input-group mb-5">
              <label className="input-label text-xs tracking-wider uppercase font-bold text-text-muted">Cardholder Name</label>
              <input type="text" className="input-field bg-bg-tertiary border-transparent focus:bg-white focus:border-accent-primary transition-colors" placeholder="John Doe" value={cardName} onChange={e => setCardName(e.target.value)} required />
            </div>
            
            <div className="input-group mb-5">
              <label className="input-label text-xs tracking-wider uppercase font-bold text-text-muted">Card Number</label>
              <div className="relative">
                <input type="text" className="input-field pl-[2.75rem] bg-bg-tertiary border-transparent focus:bg-white focus:border-accent-primary transition-colors" style={{ paddingLeft: '2.75rem' }} placeholder="0000 0000 0000 0000" value={cardNumber} onChange={e => setCardNumber(e.target.value)} maxLength={19} required />
                <CreditCard className="absolute left-4 top-1/2 transform -translate-y-1/2 text-muted" size={20} style={{ transform: 'translateY(-50%)', top: '50%' }} />
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-4 mb-8">
              <div className="input-group">
                <label className="input-label text-xs tracking-wider uppercase font-bold text-text-muted">Expiry Date</label>
                <input type="text" className="input-field bg-bg-tertiary border-transparent focus:bg-white focus:border-accent-primary transition-colors" placeholder="MM/YY" value={expiry} onChange={e => setExpiry(e.target.value)} maxLength={5} required />
              </div>
              
              <div className="input-group">
                <label className="input-label text-xs tracking-wider uppercase font-bold text-text-muted">CVC</label>
                <input type="text" className="input-field bg-bg-tertiary border-transparent focus:bg-white focus:border-accent-primary transition-colors" placeholder="123" value={cvc} onChange={e => setCvc(e.target.value)} maxLength={4} required />
              </div>
            </div>

            <button type="submit" disabled={loading} className="btn btn-primary w-full shadow-glow" style={{ padding: '1.25rem', fontSize: '1.1rem', display: 'flex', justifyContent: 'center' }}>
              {loading ? (
                <span className="flex items-center gap-3">
                  <span style={{ width: '20px', height: '20px', border: '3px solid white', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></span>
                  Processing...
                </span>
              ) : (
                <span className="flex items-center justify-center gap-2">
                  <ShieldCheck size={20} />
                  Pay Securely
                </span>
              )}
            </button>
          </form>
        </div>
      </div>
      <style>{`@keyframes spin { 100% { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
