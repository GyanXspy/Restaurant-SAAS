import { useState, useEffect } from 'react';
import { UserService, OrderService } from '../api/services';
import { useStore } from '../store/useStore';
import { User, Package, Clock, CheckCircle, XCircle } from 'lucide-react';

export default function Profile() {
  const { user } = useStore();
  const [profile, setProfile] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      let profileData = null;
      try {
        const userRes = await UserService.getUser(user.id);
        profileData = userRes.data;
      } catch(e) {
        console.error('Failed to fetch user profile:', e);
        profileData = { ...user, status: 'ACTIVE' };
      }
      setProfile(profileData);

      let ordersData = [];
      try {
        const ordersRes = await OrderService.getCustomerOrders(user.id);
        ordersData = ordersRes.data;
      } catch(e) {
        console.error('Failed to fetch orders:', e);
        ordersData = [];
      }
      setOrders(ordersData);
    } catch (err) {
      console.error("Failed to fetch profile data.", err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusIcon = (status) => {
    switch(status) {
      case 'COMPLETED': return <CheckCircle size={16} className="text-accent-primary" />;
      case 'CANCELLED': return <XCircle size={16} className="text-danger" />;
      default: return <Clock size={16} className="text-warning" />;
    }
  };

  const getStatusClass = (status) => {
    switch(status) {
      case 'COMPLETED': return 'bg-[#e6f7ec] text-accent-secondary';
      case 'CANCELLED': return 'bg-[#fef2f2] text-danger';
      default: return 'bg-[#fef3c7] text-warning';
    }
  };

  if (loading) return <div className="container py-16 text-center mt-12 bg-bg-tertiary min-h-screen">Loading profile data...</div>;

  return (
    <div className="bg-bg-tertiary page-wrapper">
      <div className="container animate-fade-in" style={{ maxWidth: '1100px' }}>
        <h1 className="text-3xl font-bold mb-8 flex items-center gap-3 font-poppins text-text-primary">
          <User className="text-text-primary" size={32} />
          Your Profile
        </h1>

        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-1">
            <div className="card text-center" style={{ position: 'sticky', top: '6rem' }}>
              <div className="mx-auto bg-accent-light rounded-full flex items-center justify-center mb-6 shadow-sm border border-accent-glow" style={{ width: '120px', height: '120px' }}>
                <User size={56} className="text-accent-primary" />
              </div>
              <h2 className="text-2xl font-bold text-text-primary mb-1 font-poppins">{profile?.name || user.name}</h2>
              <p className="text-text-secondary mb-6 font-medium">{profile?.email || user.email}</p>
              <span className="inline-block bg-accent-light text-accent-secondary px-4 py-1.5 rounded-full font-bold text-sm tracking-wide mb-8">Premium Member</span>
              
              <div className="text-left" style={{ borderTop: '1px dashed var(--surface-border)', paddingTop: '1.5rem' }}>
                <div className="text-xs text-text-muted mb-4 uppercase tracking-widest font-bold">Account Details</div>
                <div className="flex justify-between mb-4 text-sm px-2">
                  <span className="text-text-muted font-medium">User ID</span>
                  <span className="font-mono text-text-primary font-bold">{profile?.id || user.id}</span>
                </div>
                <div className="flex justify-between mb-4 text-sm px-2">
                  <span className="text-text-muted font-medium">Phone</span>
                  <span className="text-text-primary font-bold">{profile?.phone || '+1 (555) 123-4567'}</span>
                </div>
                <div className="flex justify-between text-sm px-2">
                  <span className="text-text-muted font-medium">Status</span>
                  <span className="text-accent-primary font-bold">{profile?.status || 'Active'}</span>
                </div>
              </div>
            </div>
          </div>

          <div className="lg:col-span-2">
            <div className="card h-full">
              <h2 className="text-xl mb-8 flex items-center gap-3 pb-4 font-bold font-poppins text-text-primary" style={{ borderBottom: '1px solid var(--surface-border)' }}>
                <Package size={24} className="text-text-primary" /> 
                Order History
              </h2>
              
              {orders.length === 0 ? (
                <div className="text-center py-20">
                  <Package size={64} className="mx-auto text-text-muted opacity-20 mb-6" />
                  <p className="text-xl text-text-secondary font-medium">You haven't placed any orders yet.</p>
                  <div className="mt-8">
                    <a href="/" className="btn btn-primary px-8 text-lg">Start Ordering</a>
                  </div>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                  {orders.map((order) => (
                    <div key={order.orderId} className="card" style={{ flexDirection: window.innerWidth < 640 ? 'column' : 'row', justifyContent: 'space-between', alignItems: window.innerWidth < 640 ? 'flex-start' : 'center', cursor: 'pointer', padding: '1.25rem' }}>
                      <div>
                        <div className="flex items-center gap-4 mb-3">
                          <span className="font-bold font-mono text-lg text-text-primary">#{order.orderId}</span>
                          <span className={`flex items-center gap-1.5 px-3 py-1 rounded-md ${getStatusClass(order.status)}`}>
                            {getStatusIcon(order.status)} <span className="font-bold tracking-wide text-xs">{order.status}</span>
                          </span>
                        </div>
                        <div className="text-text-secondary font-medium flex items-center gap-2 text-sm">
                          <Clock size={16} className="text-text-muted" />
                          {new Date(order.createdAt).toLocaleDateString()} at {new Date(order.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                        </div>
                      </div>
                      
                      <div className="flex flex-col sm:items-end gap-3 w-full sm:w-auto pt-4 sm:pt-0" style={{ borderTop: window.innerWidth < 640 ? '1px dashed var(--surface-border)' : 'none' }}>
                        <div className="text-left sm:text-right flex justify-between items-center sm:block w-full">
                          <div className="text-xs text-text-muted font-bold tracking-widest uppercase mb-1 hidden sm:block">Total Amount</div>
                          <div className="font-bold text-2xl text-text-primary">${order.totalAmount.toFixed(2)}</div>
                        </div>
                        <button className="btn btn-outline" style={{ padding: '0.4rem 0.75rem', fontSize: '0.8rem', marginTop: '0.5rem' }}>
                          View Details →
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
