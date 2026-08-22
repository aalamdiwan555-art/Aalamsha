import { type ReactNode, useEffect, useMemo, useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  Activity,
  ArrowDownRight,
  ArrowUpRight,
  Ban,
  Bell,
  Blocks,
  Bot,
  Check,
  ChevronRight,
  CircleHelp,
  Clock3,
  Cloud,
  Code2,
  Copy,
  Cpu,
  CreditCard,
  Database,
  Edit3,
  ExternalLink,
  Eye,
  Gauge,
  Gift,
  Globe2,
  LayoutDashboard,
  ListFilter,
  LockKeyhole,
  LogOut,
  Menu,
  Moon,
  MoreHorizontal,
  Pause,
  Play,
  Plus,
  Radio,
  RefreshCw,
  Search,
  Send,
  Server,
  Settings2,
  ShieldCheck,
  Sparkles,
  Sun,
  TabletSmartphone,
  TerminalSquare,
  Trash2,
  UserRound,
  UsersRound,
  Wifi,
  X,
  Zap,
} from 'lucide-react';
import { Link, Route, Switch, useLocation } from 'wouter';
import { ErrorBoundary } from '@/components/error-boundary';
import { Toaster } from '@/components/ui/toaster';
import { TooltipProvider } from '@/components/ui/tooltip';
import NotFound from '@/pages/not-found';

const queryClient = new QueryClient();

type Scenario = {
  id: number;
  name: string;
  description: string;
  taps: number;
  interval: number;
  duration: number;
  active: boolean;
  updated: string;
  color: string;
};
type User = {
  id: number;
  name: string;
  email: string;
  initials: string;
  device: string;
  version: string;
  lastSeen: string;
  clicks: number;
  subscription: string;
  adFree: boolean;
  banned: boolean;
};
type EventItem = { id: number; title: string; detail: string; time: string; tone: string };
type Settings = { adInterval: number; rewardAds: boolean; trialDays: number };
type AppState = {
  scenario: Scenario;
  scenarios: Scenario[];
  users: User[];
  events: EventItem[];
  running: boolean;
  clicks: number;
  rewardViews: number;
  settings: Settings;
  dark: boolean;
};

const seed: AppState = {
  scenario: { id: 1, name: 'Daily Burst', description: 'A steady pulse for daily mobile routines.', taps: 4, interval: 220, duration: 120, active: true, updated: '2m ago', color: 'cyan' },
  scenarios: [
    { id: 1, name: 'Daily Burst', description: 'A steady pulse for daily mobile routines.', taps: 4, interval: 220, duration: 120, active: true, updated: '2m ago', color: 'cyan' },
    { id: 2, name: 'Rapid Fire', description: 'High cadence for repetitive in-app actions.', taps: 12, interval: 80, duration: 45, active: true, updated: 'Yesterday', color: 'violet' },
    { id: 3, name: 'Night Watch', description: 'Low-power overnight monitoring routine.', taps: 2, interval: 1100, duration: 480, active: false, updated: '3d ago', color: 'amber' },
    { id: 4, name: 'Precision Loop', description: 'A measured pattern for accuracy-first work.', taps: 3, interval: 460, duration: 180, active: true, updated: '5d ago', color: 'mint' },
  ],
  users: [
    { id: 101, name: 'Maya Chen', email: 'maya.chen@relaymail.io', initials: 'MC', device: 'Pixel 8 Pro', version: 'v4.8.2', lastSeen: 'Just now', clicks: 18420, subscription: 'Lifetime', adFree: true, banned: false },
    { id: 102, name: 'Owen Hart', email: 'owen.hart@relaymail.io', initials: 'OH', device: 'Galaxy S24', version: 'v4.8.2', lastSeen: '4m ago', clicks: 9340, subscription: '14 days left', adFree: false, banned: false },
    { id: 103, name: 'Priya Nair', email: 'priya.nair@relaymail.io', initials: 'PN', device: 'OnePlus 12', version: 'v4.8.1', lastSeen: '21m ago', clicks: 7210, subscription: 'Trial · 2 days', adFree: false, banned: false },
    { id: 104, name: 'Caleb Stone', email: 'caleb.stone@relaymail.io', initials: 'CS', device: 'Pixel 7a', version: 'v4.7.9', lastSeen: '2h ago', clicks: 48300, subscription: 'Expired', adFree: false, banned: true },
    { id: 105, name: 'Nia Brooks', email: 'nia.brooks@relaymail.io', initials: 'NB', device: 'Xperia 1 V', version: 'v4.8.2', lastSeen: '5h ago', clicks: 2120, subscription: '7 days left', adFree: true, banned: false },
  ],
  events: [
    { id: 1, title: 'Scenario pushed globally', detail: 'Daily Burst · v18', time: '2m ago', tone: 'cyan' },
    { id: 2, title: 'Lifetime access granted', detail: 'Maya Chen', time: '18m ago', tone: 'violet' },
    { id: 3, title: 'Reward ad completed', detail: 'Owen Hart · +24h ad-free', time: '31m ago', tone: 'amber' },
    { id: 4, title: 'Device connected', detail: 'Pixel 7a · Android 14', time: '1h ago', tone: 'mint' },
    { id: 5, title: 'Scenario archived', detail: 'Weekend Sweep · v10', time: '3h ago', tone: 'slate' },
  ],
  running: false,
  clicks: 18420,
  rewardViews: 3,
  settings: { adInterval: 180, rewardAds: true, trialDays: 7 },
  dark: true,
};

function usePersistentState() {
  const [state, setState] = useState<AppState>(() => {
    try {
      const raw = localStorage.getItem('aalamsha-state');
      return raw ? { ...seed, ...JSON.parse(raw) } : seed;
    } catch {
      return seed;
    }
  });
  useEffect(() => localStorage.setItem('aalamsha-state', JSON.stringify(state)), [state]);
  return [state, setState] as const;
}

function useToastMessage() {
  const [message, setMessage] = useState('');
  const notify = (next: string) => {
    setMessage(next);
    window.setTimeout(() => setMessage(''), 2600);
  };
  return { message, notify };
}

function Logo() {
  return (
    <Link href="/" className="brand-lockup" data-testid="link-brand">
      <span className="brand-mark"><Zap size={16} strokeWidth={3} /></span>
      <span><strong>AALAMSHA</strong><small>CLICK COMMAND</small></span>
    </Link>
  );
}

function IconButton({ label, children, onClick, testId, className = '' }: { label: string; children: ReactNode; onClick: () => void; testId: string; className?: string }) {
  return <button aria-label={label} title={label} onClick={onClick} data-testid={testId} className={`icon-button ${className}`}>{children}</button>;
}

function Shell({ children, state, setState, notify }: { children: ReactNode; state: AppState; setState: React.Dispatch<React.SetStateAction<AppState>>; notify: (message: string) => void }) {
  const [location, setLocation] = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const isAdmin = location.startsWith('/admin');
  const userNav = [{ href: '/', label: 'Command center', icon: LayoutDashboard }, { href: '/profile', label: 'Profile & access', icon: UserRound }, { href: '/settings', label: 'Preferences', icon: Settings2 }];
  const adminNav = [{ href: '/admin', label: 'Overview', icon: LayoutDashboard }, { href: '/admin/scenarios', label: 'Scenarios', icon: Blocks }, { href: '/admin/users', label: 'Users & access', icon: UsersRound }, { href: '/admin/analytics', label: 'Analytics', icon: Activity }];
  const nav = isAdmin ? adminNav : userNav;
  const go = (href: string) => { setLocation(href); setMobileOpen(false); };
  const toggleTheme = () => setState(s => ({ ...s, dark: !s.dark }));
  useEffect(() => {
    document.documentElement.classList.toggle('dark', state.dark);
  }, [state.dark]);
  return (
    <div className="app-frame">
      <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-top"><Logo /><IconButton label="Close navigation" onClick={() => setMobileOpen(false)} testId="button-close-navigation" className="mobile-only"><X size={18} /></IconButton></div>
        <div className="mode-switcher" data-testid="display-mode-switcher">
          <button className={!isAdmin ? 'mode-active' : ''} onClick={() => go('/')} data-testid="button-user-console"><span className="mode-dot mode-cyan" />User Console</button>
          <button className={isAdmin ? 'mode-active' : ''} onClick={() => go('/admin')} data-testid="button-admin-console"><span className="mode-dot mode-violet" />Admin Console</button>
        </div>
        <div className="nav-label">{isAdmin ? 'Operations' : 'Workspace'}</div>
        <nav className="primary-nav" aria-label="Primary navigation">
          {nav.map(item => {
            const Icon = item.icon;
            const active = item.href === '/' ? location === '/' : location === item.href;
            return <Link href={item.href} key={item.href} className={`nav-item ${active ? 'nav-active' : ''}`} data-testid={`link-nav-${item.label.toLowerCase().replaceAll(' ', '-')}`} onClick={() => setMobileOpen(false)}><Icon size={17} /><span>{item.label}</span>{active && <ChevronRight size={14} className="nav-chevron" />}</Link>;
          })}
        </nav>
        <div className="sidebar-status">
          <div className="status-label"><span className="live-dot" />Network online</div>
          <div className="status-meta"><span>API</span><b>v2.14.0</b></div>
          <div className="status-meta"><span>Region</span><b>US-EAST-1</b></div>
        </div>
        <div className="sidebar-bottom">
          <div className="user-chip"><span className="avatar avatar-small">AD</span><span><b>Alex Duarte</b><small>Owner account</small></span><MoreHorizontal size={15} /></div>
          <button className="logout-link" onClick={() => notify('Demo session stays active')} data-testid="button-sign-out"><LogOut size={14} />Sign out</button>
        </div>
      </aside>
      {mobileOpen && <button className="scrim" onClick={() => setMobileOpen(false)} data-testid="button-close-scrim" aria-label="Close menu" />}
      <main className="main-area" onClickCapture={event => { const target = event.target as HTMLElement; if (target.closest('[data-testid="button-chart-7d"]')) notify('7 day view selected'); }}>
        <header className="topbar">
          <IconButton label="Open navigation" onClick={() => setMobileOpen(true)} testId="button-open-navigation" className="mobile-only"><Menu size={19} /></IconButton>
          <div className="breadcrumbs"><span className="crumb-muted">Aalamsha</span><ChevronRight size={13} /><span>{isAdmin ? 'Operations' : 'Workspace'}</span><ChevronRight size={13} /><b>{nav.find(n => n.href === location)?.label ?? 'Command center'}</b></div>
          <div className="topbar-actions">
            <span className="system-pill"><span className="live-dot" />All systems nominal</span>
            <IconButton label={state.dark ? 'Switch to light theme' : 'Switch to dark theme'} onClick={toggleTheme} testId="button-toggle-theme">{state.dark ? <Sun size={17} /> : <Moon size={17} />}</IconButton>
            <IconButton label="Notifications" onClick={() => notify('No new alerts')} testId="button-notifications"><Bell size={17} /><i className="notification-dot" /></IconButton>
          </div>
        </header>
        <div className="content-wrap">{children}</div>
      </main>
    </div>
  );
}

function Toast({ message }: { message: string }) {
  if (!message) return null;
  return <div className="toast-message" role="status" data-testid="status-toast"><Check size={16} />{message}</div>;
}

function SectionHeading({ eyebrow, title, detail, action }: { eyebrow: string; title: string; detail: string; action?: ReactNode }) {
  return <div className="section-heading"><div><div className="eyebrow">{eyebrow}</div><h1 data-testid={`text-page-title-${title.toLowerCase().replaceAll(' ', '-')}`}>{title}</h1><p>{detail}</p></div>{action}</div>;
}

function Metric({ label, value, delta, icon: Icon, tone = 'cyan' }: { label: string; value: string; delta?: string; icon: typeof Activity; tone?: string }) {
  return <div className={`metric-card tone-${tone}`} data-testid={`metric-${label.toLowerCase().replaceAll(' ', '-')}`}><div className="metric-top"><span>{label}</span><span className="metric-icon"><Icon size={16} /></span></div><strong>{value}</strong>{delta && <div className="metric-delta"><ArrowUpRight size={13} />{delta}</div>}</div>;
}

function StatusBadge({ children, tone = 'cyan' }: { children: ReactNode; tone?: string }) {
  return <span className={`status-badge badge-${tone}`} data-testid="status-badge">{children}</span>;
}

function ProgressBar({ value, tone = 'cyan' }: { value: number; tone?: string }) {
  return <div className="progress-track"><span className={`progress-fill fill-${tone}`} style={{ width: `${Math.min(value, 100)}%` }} /></div>;
}

function Dashboard({ state, setState, notify }: { state: AppState; setState: React.Dispatch<React.SetStateAction<AppState>>; notify: (message: string) => void }) {
  const toggleRunning = () => {
    setState(s => ({ ...s, running: !s.running, clicks: s.running ? s.clicks : s.clicks + 4, events: s.running ? s.events : [{ id: Date.now(), title: 'Click engine started', detail: `${s.scenario.name} · local device`, time: 'Just now', tone: 'cyan' }, ...s.events] }));
    notify(state.running ? 'Click engine paused' : 'Click engine active');
  };
  const watchAd = () => {
    setState(s => ({ ...s, rewardViews: Math.min(5, s.rewardViews + 1), events: [{ id: Date.now(), title: 'Reward ad completed', detail: '24h ad-free access earned', time: 'Just now', tone: 'amber' }, ...s.events] }));
    notify('Reward complete · 24 hours ad-free added');
  };
  return <div className="page page-dashboard">
    <SectionHeading eyebrow="USER CONSOLE / 04:18 UTC" title="Command center" detail="Your click engine, tuned and ready." action={<div className="header-tag"><Radio size={14} />Local device synced</div>} />
    <div className="user-hero-grid">
      <section className={`engine-card ${state.running ? 'engine-live' : ''}`}>
        <div className="engine-grid" />
        <div className="engine-card-top"><span className="eyebrow">ACTIVE SCENARIO</span><StatusBadge tone={state.running ? 'mint' : 'slate'}>{state.running ? 'RUNNING' : 'STANDBY'}</StatusBadge></div>
        <div className="engine-core"><div className="core-ring ring-outer"><div className="core-ring ring-inner"><div className="core-center">{state.running ? <Activity size={27} /> : <Pause size={26} />}<small>{state.running ? 'PULSING' : 'PAUSED'}</small></div></div></div></div>
        <div className="engine-title"><h2 data-testid="text-current-scenario">{state.scenario.name}</h2><p>{state.scenario.description}</p></div>
        <button className={`engine-button ${state.running ? 'button-stop' : ''}`} onClick={toggleRunning} data-testid="button-toggle-clicker">{state.running ? <><Pause size={18} />Stop clicker</> : <><Play size={18} />Start clicker</>}</button>
        <div className="engine-specs"><span><b>{state.scenario.taps}</b> taps / burst</span><span><b>{state.scenario.interval}ms</b> interval</span><span><b>{state.scenario.duration}s</b> session</span></div>
      </section>
      <div className="dashboard-side">
        <div className="panel scenario-panel">
          <div className="panel-heading"><div><span className="eyebrow">SCENARIO</span><h3>Current protocol</h3></div><Link href="/admin/scenarios" className="text-link" data-testid="link-manage-scenarios">Manage <ChevronRight size={14} /></Link></div>
          <div className="scenario-live"><span className="scenario-symbol"><TerminalSquare size={19} /></span><div><b data-testid="text-scenario-name">{state.scenario.name}</b><small>Synced {state.scenario.updated}</small></div><StatusBadge>{state.scenario.active ? 'ACTIVE' : 'PAUSED'}</StatusBadge></div>
          <div className="scenario-rule"><span>Pattern density</span><div className="mini-bars">{[40, 74, 54, 92, 68, 81, 52, 72].map((height, i) => <i key={i} style={{ height: `${height}%` }} />)}</div><b>82%</b></div>
        </div>
        <div className="panel access-panel">
          <div className="panel-heading"><div><span className="eyebrow">ACCESS STATUS</span><h3>Lifetime access</h3></div><ShieldCheck className="icon-mint" size={20} /></div>
          <div className="access-row"><span className="access-icon"><LockKeyhole size={16} /></span><div><b>Ad-free forever</b><small>Owner-granted access is active</small></div><StatusBadge tone="mint">ACTIVE</StatusBadge></div>
          <div className="access-row"><span className="access-icon"><Cloud size={16} /></span><div><b>Cloud sync</b><small>Last synced 2 minutes ago</small></div><span className="sync-check"><Check size={14} /></span></div>
        </div>
      </div>
    </div>
    <div className="dashboard-grid">
      <section className="panel reward-panel"><div className="panel-heading"><div><span className="eyebrow">REWARD LOOP</span><h3>Keep your streak clear</h3></div><Gift className="icon-amber" size={20} /></div><p className="panel-copy">Watch a short rewarded ad to unlock 24 hours without interruptions. No payment required.</p><div className="reward-progress"><div className="reward-number"><strong data-testid="display-reward-progress">{state.rewardViews}</strong><span>/ 5<br />this week</span></div><ProgressBar value={state.rewardViews * 20} tone="amber" /></div><button className="secondary-button" onClick={watchAd} disabled={state.rewardViews >= 5} data-testid="button-watch-reward-ad"><Eye size={15} />{state.rewardViews >= 5 ? 'Weekly reward complete' : 'Watch rewarded ad'}<ChevronRight size={15} /></button></section>
      <section className="panel activity-panel"><div className="panel-heading"><div><span className="eyebrow">LOCAL TELEMETRY</span><h3>Click activity</h3></div><span className="live-label"><span className="live-dot" />Live</span></div><div className="activity-number"><strong data-testid="display-total-clicks">{state.clicks.toLocaleString()}</strong><span>total taps<br /><em>+12.8% this week</em></span></div><div className="sparkline" aria-label="Click activity chart">{[28, 34, 30, 45, 40, 63, 52, 72, 60, 77, 70, 88, 79, 94].map((height, index) => <i key={index} style={{ height: `${height}%` }} />)}</div><div className="chart-axis"><span>MON</span><span>WED</span><span>FRI</span><span>SUN</span></div></section>
      <section className="panel quick-panel"><div className="panel-heading"><div><span className="eyebrow">QUICK READ</span><h3>Engine health</h3></div><Gauge className="icon-cyan" size={20} /></div><div className="health-score"><div className="health-dial"><strong>98</strong><small>/100</small></div><div><b>All systems nominal</b><p>Latency is within your target range.</p></div></div><div className="health-list"><span><i className="health-dot mint-dot" />Accessibility service <b>Ready</b></span><span><i className="health-dot cyan-dot" />Battery impact <b>Low</b></span></div></section>
    </div>
    <div className="event-strip"><span className="eyebrow">RECENT ACTIVITY</span>{state.events.slice(0, 3).map(event => <div className="event-inline" key={event.id}><span className={`event-dot ${event.tone}`} /><b>{event.title}</b><span>{event.time}</span></div>)}<button className="ghost-button" onClick={() => notify('Activity log is up to date')} data-testid="button-refresh-activity"><RefreshCw size={14} />Refresh</button></div>
  </div>;
}

function AdminOverview({ state, notify }: { state: AppState; notify: (message: string) => void }) {
  return <div className="page"><SectionHeading eyebrow="ADMIN CONSOLE / OVERVIEW" title="Operations overview" detail="A live read on the Aalamsha click network." action={<button className="secondary-button compact" onClick={() => notify('Dashboard data refreshed')} data-testid="button-refresh-dashboard"><RefreshCw size={14} />Refresh data</button>} />
    <div className="metrics-grid"><Metric label="Active users" value="1,284" delta="+8.4% vs last week" icon={UsersRound} /><Metric label="Running scenarios" value="692" delta="+5.1% today" icon={Zap} tone="violet" /><Metric label="Estimated ad revenue" value="$4,817.20" delta="+12.8% this month" icon={CreditCard} tone="amber" /><Metric label="Clicks today" value="2.84M" delta="+18.3% vs yesterday" icon={Activity} tone="mint" /></div>
    <div className="admin-main-grid"><section className="panel large-chart"><div className="panel-heading"><div><span className="eyebrow">NETWORK PULSE</span><h3>Click activity</h3></div><div className="chart-controls"><button className="chart-tab active" data-testid="button-chart-7d">7D</button><button className="chart-tab" onClick={() => notify('30 day view selected')} data-testid="button-chart-30d">30D</button><button className="chart-tab" onClick={() => notify('90 day view selected')} data-testid="button-chart-90d">90D</button></div></div><div className="chart-legend"><span><i className="legend-dot cyan" />Clicks</span><span><i className="legend-dot violet" />Sessions</span><b>2.84M <small>today</small></b></div><div className="admin-chart"><div className="grid-lines"><i /><i /><i /><i /></div><svg viewBox="0 0 700 220" preserveAspectRatio="none" role="img" aria-label="Network click activity"><defs><linearGradient id="areaCyan" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor="#2de2e6" stopOpacity=".24" /><stop offset="1" stopColor="#2de2e6" stopOpacity="0" /></linearGradient></defs><path d="M0,174 C40,165 46,137 92,148 S136,161 178,115 S224,126 264,96 S307,116 348,104 S394,67 440,84 S480,113 524,67 S568,76 608,45 S656,57 700,22 L700,220 L0,220Z" fill="url(#areaCyan)" /><path d="M0,174 C40,165 46,137 92,148 S136,161 178,115 S224,126 264,96 S307,116 348,104 S394,67 440,84 S480,113 524,67 S568,76 608,45 S656,57 700,22" fill="none" stroke="#2de2e6" strokeWidth="2.5" /><path d="M0,195 C45,188 49,171 92,176 S137,180 178,158 S224,170 264,139 S308,156 348,147 S395,118 440,134 S480,158 524,121 S568,132 608,100 S658,115 700,83" fill="none" stroke="#9b82f4" strokeWidth="1.5" strokeDasharray="5 6" /></svg><div className="chart-labels"><span>06:00</span><span>10:00</span><span>14:00</span><span>18:00</span><span>NOW</span></div></div></section><section className="panel events-panel"><div className="panel-heading"><div><span className="eyebrow">AUDIT STREAM</span><h3>Recent events</h3></div><button className="icon-button" onClick={() => notify('Event stream is live')} data-testid="button-event-filter"><ListFilter size={16} /></button></div><div className="event-list">{state.events.map(event => <div className="event-item" key={event.id}><span className={`event-icon ${event.tone}`}><Activity size={14} /></span><div><b data-testid={`text-event-${event.id}`}>{event.title}</b><small>{event.detail}</small></div><time>{event.time}</time></div>)}</div><button className="text-link full-link" onClick={() => notify('Showing all audit events')} data-testid="button-view-all-events">View all events <ArrowUpRight size={14} /></button></section></div>
    <div className="admin-lower-grid"><section className="panel health-panel"><div className="panel-heading"><div><span className="eyebrow">INFRASTRUCTURE</span><h3>Fleet health</h3></div><StatusBadge tone="mint">99.98% UPTIME</StatusBadge></div><div className="infra-row"><span><Server size={16} />Click relay</span><div className="infra-bar"><i style={{ width: '94%' }} /></div><b>94ms</b></div><div className="infra-row"><span><Database size={16} />Scenario sync</span><div className="infra-bar violet"><i style={{ width: '98%' }} /></div><b>42ms</b></div><div className="infra-row"><span><Wifi size={16} />Device handshake</span><div className="infra-bar amber"><i style={{ width: '87%' }} /></div><b>128ms</b></div></section><section className="panel distribution-panel"><div className="panel-heading"><div><span className="eyebrow">SCENARIO DISTRIBUTION</span><h3>What the network runs</h3></div><Link href="/admin/scenarios" className="text-link" data-testid="link-overview-scenarios">View library <ChevronRight size={14} /></Link></div>{state.scenarios.slice(0, 4).map((s, index) => <div className="distribution-row" key={s.id}><span className={`scenario-symbol tiny ${s.color}`}><TerminalSquare size={14} /></span><b>{s.name}</b><div className="distribution-bar"><i className={`fill-${s.color}`} style={{ width: `${[68, 49, 22, 36][index]}%` }} /></div><span>{[68, 49, 22, 36][index]}%</span></div>)}</section></div>
  </div>;
}

function Dialog({ title, description, children, onClose }: { title: string; description: string; children: ReactNode; onClose: () => void }) {
  useEffect(() => { const onKey = (event: KeyboardEvent) => event.key === 'Escape' && onClose(); document.addEventListener('keydown', onKey); return () => document.removeEventListener('keydown', onKey); }, [onClose]);
  return <div className="dialog-backdrop" role="presentation" onMouseDown={event => event.currentTarget === event.target && onClose()}><div className="dialog-card" role="dialog" aria-modal="true" aria-labelledby="dialog-title"><div className="dialog-head"><div><span className="eyebrow">CONFIGURE</span><h2 id="dialog-title">{title}</h2><p>{description}</p></div><IconButton label="Close dialog" onClick={onClose} testId="button-close-dialog"><X size={17} /></IconButton></div>{children}</div></div>;
}

function ScenarioManager({ state, setState, notify }: { state: AppState; setState: React.Dispatch<React.SetStateAction<AppState>>; notify: (message: string) => void }) {
  const [selected, setSelected] = useState(state.scenarios[0].id);
  const [dialog, setDialog] = useState<'create' | 'edit' | null>(null);
  const [form, setForm] = useState<Scenario>(state.scenarios[0]);
  const current = state.scenarios.find(s => s.id === selected) ?? state.scenarios[0];
  const openEdit = (scenario: Scenario) => { setForm(scenario); setSelected(scenario.id); setDialog('edit'); };
  const saveScenario = () => {
    if (!form.name.trim()) return;
    const nextId = dialog === 'create' ? Date.now() : form.id;
    const nextScenario = { ...form, id: nextId, updated: 'Just now' };
    setState(s => ({ ...s, scenarios: dialog === 'create' ? [...s.scenarios, nextScenario] : s.scenarios.map(item => item.id === form.id ? nextScenario : item), scenario: s.scenario.id === form.id ? nextScenario : s.scenario }));
    setSelected(nextId); setDialog(null); notify(dialog === 'create' ? 'Scenario created' : 'Scenario updated');
  };
  const deleteScenario = (id: number) => {
    if (state.scenarios.length <= 1) { notify('Keep at least one scenario active'); return; }
    setState(s => ({ ...s, scenarios: s.scenarios.filter(item => item.id !== id), scenario: s.scenario.id === id ? s.scenarios.find(item => item.id !== id)! : s.scenario }));
    setSelected(state.scenarios.find(item => item.id !== id)?.id ?? 0); notify('Scenario deleted');
  };
  const toggleActive = (id: number) => { setState(s => ({ ...s, scenarios: s.scenarios.map(item => item.id === id ? { ...item, active: !item.active, updated: 'Just now' } : item) })); notify('Scenario state updated'); };
  const pushGlobal = () => { setState(s => ({ ...s, scenario: current, events: [{ id: Date.now(), title: 'Scenario pushed globally', detail: `${current.name} · all active devices`, time: 'Just now', tone: 'cyan' }, ...s.events] })); notify(`${current.name} pushed to all devices`); };
  return <div className="page"><SectionHeading eyebrow="ADMIN CONSOLE / SCENARIO LIBRARY" title="Scenario manager" detail="Compose the exact rhythm your users need." action={<button className="primary-button" onClick={() => { setForm({ id: Date.now(), name: '', description: '', taps: 4, interval: 250, duration: 120, active: true, updated: 'Just now', color: 'cyan' }); setDialog('create'); }} data-testid="button-create-scenario"><Plus size={16} />New scenario</button>} />
    <div className="scenario-layout"><section className="panel scenario-list-panel"><div className="list-toolbar"><div><span className="eyebrow">LIBRARY</span><h3>{state.scenarios.length} protocols</h3></div><button className="filter-button" onClick={() => notify('Showing all scenarios')} data-testid="button-filter-scenarios"><ListFilter size={15} />All</button></div><div className="scenario-list">{state.scenarios.map(item => <button className={`scenario-list-item ${selected === item.id ? 'selected' : ''}`} onClick={() => setSelected(item.id)} key={item.id} data-testid={`button-select-scenario-${item.id}`}><span className={`scenario-symbol ${item.color}`}><TerminalSquare size={17} /></span><span className="scenario-list-copy"><b>{item.name}</b><small>{item.taps} taps · {item.interval}ms · {item.duration}s</small></span><span className={`list-state ${item.active ? 'active' : ''}`} /><ChevronRight size={15} /></button>)}</div><div className="empty-hint"><Sparkles size={15} /><span>Tip: Keep protocols focused. One job, one rhythm.</span></div></section><section className="panel builder-panel"><div className="builder-top"><div><span className="eyebrow">BUILDER / PROTOCOL {String(current.id).padStart(2, '0')}</span><h2 data-testid="text-builder-scenario">{current.name}</h2><p>{current.description}</p></div><div className="builder-actions"><button className="icon-button" onClick={() => openEdit(current)} data-testid={`button-edit-scenario-${current.id}`}><Edit3 size={16} /></button><button className="icon-button danger-icon" onClick={() => deleteScenario(current.id)} data-testid={`button-delete-scenario-${current.id}`}><Trash2 size={16} /></button></div></div><div className="builder-preview"><div className="preview-orbit orbit-a" /><div className="preview-orbit orbit-b" /><div className="preview-core"><Cpu size={28} /><span>LOOP</span></div><div className="preview-stat stat-one"><b>{current.taps}</b><small>taps / burst</small></div><div className="preview-stat stat-two"><b>{current.interval}<em>ms</em></b><small>interval</small></div><div className="preview-stat stat-three"><b>{current.duration}<em>s</em></b><small>duration</small></div></div><div className="builder-fields"><div><span>Cadence</span><b>{current.interval} <small>milliseconds</small></b></div><div><span>Actions per burst</span><b>{current.taps} <small>tap events</small></b></div><div><span>Session window</span><b>{Math.floor(current.duration / 60)}:{String(current.duration % 60).padStart(2, '0')} <small>minutes</small></b></div></div><div className="builder-footer"><div><span className="eyebrow">DISTRIBUTION STATE</span><strong><span className={`live-dot ${current.active ? '' : 'offline-dot'}`} />{current.active ? 'Available to users' : 'Paused from library'}</strong></div><div className="builder-footer-actions"><button className="secondary-button" onClick={() => toggleActive(current.id)} data-testid={`button-toggle-scenario-${current.id}`}>{current.active ? <Pause size={15} /> : <Play size={15} />}{current.active ? 'Pause scenario' : 'Activate scenario'}</button><button className="primary-button" onClick={pushGlobal} data-testid="button-push-scenario"><Send size={15} />Push globally</button></div></div></section></div>
    {dialog && <Dialog title={dialog === 'create' ? 'New scenario' : 'Edit scenario'} description="Set a rhythm. You can change or pause it at any time." onClose={() => setDialog(null)}><div className="form-grid"><label className="field full"><span>Scenario name</span><input autoFocus value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="e.g. Morning routine" data-testid="input-scenario-name" /></label><label className="field full"><span>Description</span><textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="What is this protocol for?" data-testid="input-scenario-description" /></label><label className="field"><span>Taps / burst</span><input type="number" value={form.taps} onChange={e => setForm({ ...form, taps: Number(e.target.value) })} data-testid="input-scenario-taps" /></label><label className="field"><span>Interval (ms)</span><input type="number" value={form.interval} onChange={e => setForm({ ...form, interval: Number(e.target.value) })} data-testid="input-scenario-interval" /></label><label className="field"><span>Duration (sec)</span><input type="number" value={form.duration} onChange={e => setForm({ ...form, duration: Number(e.target.value) })} data-testid="input-scenario-duration" /></label><label className="field"><span>Accent</span><select value={form.color} onChange={e => setForm({ ...form, color: e.target.value })} data-testid="select-scenario-color"><option value="cyan">Cyan</option><option value="violet">Violet</option><option value="amber">Amber</option><option value="mint">Mint</option></select></label></div><div className="dialog-footer"><button className="secondary-button" onClick={() => setDialog(null)} data-testid="button-cancel-scenario">Cancel</button><button className="primary-button" onClick={saveScenario} data-testid="button-save-scenario"><Check size={15} />Save scenario</button></div></Dialog>}
  </div>;
}

function UserManager({ state, setState, notify }: { state: AppState; setState: React.Dispatch<React.SetStateAction<AppState>>; notify: (message: string) => void }) {
  const [search, setSearch] = useState('');
  const [grantUser, setGrantUser] = useState<User | null>(null);
  const [days, setDays] = useState('30');
  const filtered = useMemo(() => state.users.filter(user => `${user.name} ${user.email} ${user.device}`.toLowerCase().includes(search.toLowerCase())), [state.users, search]);
  const toggleUser = (id: number, key: 'adFree' | 'banned') => { setState(s => ({ ...s, users: s.users.map(user => user.id === id ? { ...user, [key]: !user[key] } : user) })); notify(key === 'banned' ? 'User access state updated' : 'Ad-free access updated'); };
  const grant = () => { if (!grantUser) return; setState(s => ({ ...s, users: s.users.map(user => user.id === grantUser.id ? { ...user, subscription: days === 'lifetime' ? 'Lifetime' : `${days} days left` } : user), events: [{ id: Date.now(), title: days === 'lifetime' ? 'Lifetime access granted' : 'Subscription granted', detail: `${grantUser.name} · admin action`, time: 'Just now', tone: 'violet' }, ...s.events] })); setGrantUser(null); notify('Access granted by admin'); };
  return <div className="page"><SectionHeading eyebrow="ADMIN CONSOLE / ACCESS CONTROL" title="User management" detail="Keep every device in the right state, at a glance." action={<div className="header-tag"><ShieldCheck size={14} />Admin-only controls</div>} /><div className="panel users-panel"><div className="users-toolbar"><div className="search-field"><Search size={16} /><input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search name, email, or device" data-testid="input-search-users" /></div><div className="toolbar-actions"><button className="filter-button" onClick={() => notify('Filter menu ready')} data-testid="button-filter-users"><ListFilter size={15} />Filter <span className="filter-count">5</span></button><button className="icon-button" onClick={() => notify('User list exported')} data-testid="button-export-users"><ExternalLink size={16} /></button></div></div><div className="user-table-wrap"><table className="user-table"><thead><tr><th>User</th><th>Device</th><th>Last active</th><th>Clicks</th><th>Subscription</th><th>Controls</th></tr></thead><tbody>{filtered.map(user => <tr key={user.id} className={user.banned ? 'row-banned' : ''}><td><div className="table-user"><span className={`avatar ${user.banned ? 'avatar-banned' : ''}`}>{user.initials}</span><span><b data-testid={`text-user-name-${user.id}`}>{user.name}</b><small>{user.email}</small></span></div></td><td><div className="device-cell"><TabletSmartphone size={15} /><span>{user.device}<small>{user.version}</small></span></div></td><td><span className="last-seen"><i className={user.lastSeen === 'Just now' ? 'live-dot' : 'offline-dot'} />{user.lastSeen}</span></td><td><b className="mono-value">{user.clicks.toLocaleString()}</b></td><td><StatusBadge tone={user.subscription === 'Lifetime' ? 'mint' : user.subscription === 'Expired' ? 'slate' : 'violet'}>{user.subscription}</StatusBadge>{user.adFree && <small className="ad-free-label"><LockKeyhole size={11} />ad-free</small>}</td><td><div className="row-actions"><button className="table-action" onClick={() => { setGrantUser(user); setDays('30'); }} data-testid={`button-grant-user-${user.id}`}><CreditCard size={14} />Grant</button><button className={`table-action ${user.adFree ? 'is-on' : ''}`} onClick={() => toggleUser(user.id, 'adFree')} data-testid={`button-toggle-adfree-${user.id}`}><LockKeyhole size={14} />Ad-free</button><button className={`table-action ${user.banned ? 'is-danger' : ''}`} onClick={() => toggleUser(user.id, 'banned')} data-testid={`button-ban-user-${user.id}`}>{user.banned ? <Check size={14} /> : <Ban size={14} />}{user.banned ? 'Unban' : 'Ban'}</button></div></td></tr>)}</tbody></table>{filtered.length === 0 && <div className="empty-state"><Search size={24} /><h3>No users found</h3><p>Try a different name, email, or device.</p></div>}</div><div className="table-footer"><span>Showing <b>{filtered.length}</b> of {state.users.length} users</span><span className="mono-label">SYNCED 04:18:22 UTC</span></div></div>{grantUser && <Dialog title={`Grant access · ${grantUser.name}`} description="Admin-granted access is always free for the recipient." onClose={() => setGrantUser(null)}><div className="grant-options">{[['7', '7 days', 'A short runway'], ['30', '30 days', 'A monthly cycle'], ['90', '90 days', 'A longer session'], ['lifetime', 'Lifetime', 'No expiry']].map(([value, label, detail]) => <button className={`grant-option ${days === value ? 'selected' : ''}`} onClick={() => setDays(value)} key={value} data-testid={`button-grant-duration-${value}`}><span className="grant-radio">{days === value && <i />}</span><span><b>{label}</b><small>{detail}</small></span><ChevronRight size={15} /></button>)}</div><div className="dialog-footer"><button className="secondary-button" onClick={() => setGrantUser(null)} data-testid="button-cancel-grant">Cancel</button><button className="primary-button" onClick={grant} data-testid="button-confirm-grant"><ShieldCheck size={15} />Grant access</button></div></Dialog>}</div>;
}

function Analytics({ notify }: { notify: (message: string) => void }) {
  const bars = [48, 62, 54, 72, 66, 84, 78, 92, 75, 88, 81, 96, 90, 98];
  return <div className="page"><SectionHeading eyebrow="ADMIN CONSOLE / TELEMETRY" title="Analytics & revenue" detail="Understand the rhythms powering the network." action={<button className="secondary-button compact" onClick={() => notify('Report export queued')} data-testid="button-export-report"><ExternalLink size={14} />Export report</button>} /><div className="analytics-summary"><Metric label="Ad impressions" value="8.42M" delta="+16.2% this month" icon={Eye} /><Metric label="Reward completions" value="642K" delta="+9.7% vs last month" icon={Gift} tone="amber" /><Metric label="Fill rate" value="93.8%" delta="+2.4% this month" icon={Gauge} tone="mint" /><Metric label="Avg. session" value="11m 42s" delta="+1m 08s" icon={Clock3} tone="violet" /></div><div className="analytics-grid"><section className="panel revenue-chart"><div className="panel-heading"><div><span className="eyebrow">MONETIZATION / ESTIMATE</span><h3>Ad performance</h3></div><div className="period-select">This month <ChevronRight size={14} /></div></div><div className="revenue-head"><strong>$4,817.20</strong><span><ArrowUpRight size={14} />12.8% <small>vs prior period</small></span></div><div className="bar-chart">{bars.map((height, index) => <div className="bar-column" key={index}><i style={{ height: `${height}%` }} /><small>{['01', '', '', '04', '', '', '07', '', '', '10', '', '', '13', ''][index]}</small></div>)}</div><div className="chart-axis wide"><span>01 MAY</span><span>07 MAY</span><span>14 MAY</span></div></section><section className="panel breakdown-panel"><div className="panel-heading"><div><span className="eyebrow">MIX</span><h3>Revenue breakdown</h3></div><button className="icon-button" onClick={() => notify('Breakdown copied')} data-testid="button-copy-breakdown"><Copy size={15} /></button></div><div className="donut-wrap"><div className="donut"><div><strong>100%</strong><small>ad-funded</small></div></div><div className="breakdown-list"><span><i className="legend-dot cyan" />Banner <b>41%</b></span><span><i className="legend-dot violet" />Interstitial <b>34%</b></span><span><i className="legend-dot amber" />Rewarded <b>25%</b></span></div></div><p className="fine-print">Aalamsha has no user-facing monetary plans. Revenue is solely estimated from the free ad-supported experience.</p></section></div><section className="panel acquisition-panel"><div className="panel-heading"><div><span className="eyebrow">NETWORK HEALTH</span><h3>Device cohort performance</h3></div><StatusBadge tone="mint">STABLE</StatusBadge></div><div className="cohort-grid"><div><span>Android 14+</span><ProgressBar value={82} /><b>82% <small>of sessions</small></b></div><div><span>Returning users</span><ProgressBar value={64} tone="violet" /><b>64% <small>7d retention</small></b></div><div><span>Ad-free access</span><ProgressBar value={28} tone="amber" /><b>28% <small>of active users</small></b></div><div><span>Reward streaks</span><ProgressBar value={47} tone="mint" /><b>47.2% <small>completed</small></b></div></div></section></div>;
}

function Profile({ state, notify }: { state: AppState; notify: (message: string) => void }) {
  return <div className="page narrow-page"><SectionHeading eyebrow="USER CONSOLE / IDENTITY" title="Profile & access" detail="Your account footprint across the click network." action={<StatusBadge tone="mint">VERIFIED</StatusBadge>} /><section className="profile-hero panel"><div className="profile-avatar">AD<span className="verified-mark"><Check size={11} /></span></div><div className="profile-copy"><span className="eyebrow">OWNER ACCOUNT</span><h2 data-testid="text-profile-name">Alex Duarte</h2><p data-testid="text-profile-email">alex.duarte@aalamsha.dev</p></div><button className="secondary-button" onClick={() => notify('Profile editing is ready')} data-testid="button-edit-profile"><Edit3 size={14} />Edit profile</button></section><div className="profile-grid"><section className="panel"><div className="panel-heading"><div><span className="eyebrow">ACCESS TIER</span><h3>Lifetime access</h3></div><ShieldCheck className="icon-mint" size={20} /></div><div className="tier-card"><div className="tier-icon"><Sparkles size={20} /></div><div><b>Owner-granted lifetime</b><p>Ad-free access with every Aalamsha protocol.</p></div><StatusBadge tone="mint">ACTIVE</StatusBadge></div><div className="detail-list"><span><b>Granted on</b><span>18 Mar 2024</span></span><span><b>Next review</b><span>Never</span></span><span><b>Source</b><span>Admin console</span></span></div></section><section className="panel"><div className="panel-heading"><div><span className="eyebrow">DEVICE PROFILE</span><h3>Primary device</h3></div><TabletSmartphone className="icon-cyan" size={20} /></div><div className="device-large"><span className="device-icon"><TabletSmartphone size={21} /></span><div><b>Pixel 8 Pro</b><p>Android 14 · Aalamsha v4.8.2</p></div><StatusBadge>SYNCED</StatusBadge></div><div className="detail-list"><span><b>Last sync</b><span>2 minutes ago</span></span><span><b>Local clicks</b><span data-testid="text-profile-clicks">{state.clicks.toLocaleString()}</span></span><span><b>Battery impact</b><span className="value-mint">Low</span></span></div></section></div><section className="danger-zone panel"><div><span className="eyebrow">ACCOUNT CONTROLS</span><h3>Keep your account safe</h3><p>Manage sessions and access credentials for this workspace.</p></div><div className="danger-actions"><button className="secondary-button" onClick={() => notify('All other sessions revoked')} data-testid="button-revoke-sessions"><LockKeyhole size={14} />Revoke other sessions</button><button className="ghost-button danger-text" onClick={() => notify('Demo account cannot be deleted')} data-testid="button-delete-account"><Trash2 size={14} />Delete account</button></div></section></div>;
}

function SettingsPage({ state, setState, notify }: { state: AppState; setState: React.Dispatch<React.SetStateAction<AppState>>; notify: (message: string) => void }) {
  const [draft, setDraft] = useState(state.settings);
  const save = () => { setState(s => ({ ...s, settings: draft })); notify('Global settings saved'); };
  return <div className="page narrow-page"><SectionHeading eyebrow="ADMIN CONSOLE / SYSTEM SETTINGS" title="Global settings" detail="The defaults every new Aalamsha session inherits." action={<StatusBadge tone="amber">ADMIN ONLY</StatusBadge>} /><section className="panel settings-panel"><div className="settings-section"><div className="settings-title"><span className="setting-icon cyan"><Clock3 size={18} /></span><div><h3>Ad interval</h3><p>How often standard users see a non-rewarded ad during a session.</p></div></div><div className="range-setting"><div className="range-value"><strong data-testid="display-ad-interval">{draft.adInterval}s</strong><span>between ad opportunities</span></div><input type="range" min="30" max="600" step="30" value={draft.adInterval} onChange={e => setDraft({ ...draft, adInterval: Number(e.target.value) })} data-testid="input-ad-interval" /><div className="range-labels"><span>30 sec</span><span>10 min</span></div></div></div><div className="settings-section"><div className="settings-title"><span className="setting-icon amber"><Gift size={18} /></span><div><h3>Rewarded ads</h3><p>Allow users to unlock 24 hours of ad-free access by watching a reward.</p></div></div><button className={`toggle-control ${draft.rewardAds ? 'on' : ''}`} onClick={() => setDraft({ ...draft, rewardAds: !draft.rewardAds })} data-testid="button-toggle-reward-ads"><span /><b>{draft.rewardAds ? 'Enabled' : 'Disabled'}</b></button></div><div className="settings-section"><div className="settings-title"><span className="setting-icon violet"><Clock3 size={18} /></span><div><h3>New user trial</h3><p>Default trial duration before admin-granted access is needed.</p></div></div><div className="number-setting"><input type="number" min="0" max="30" value={draft.trialDays} onChange={e => setDraft({ ...draft, trialDays: Number(e.target.value) })} data-testid="input-trial-days" /><span>days</span></div></div><div className="settings-save"><div><Check size={15} />Changes are saved locally for this demo</div><button className="primary-button" onClick={save} data-testid="button-save-settings"><Check size={15} />Save global settings</button></div></section><section className="settings-note"><CircleHelp size={16} /><div><b>Keep it free, keep it useful.</b><p>Aalamsha never displays payment plans or checkout flows. Access is granted by admins or earned through reward ads.</p></div></section></div>;
}

function RouterContent({ state, setState, notify }: { state: AppState; setState: React.Dispatch<React.SetStateAction<AppState>>; notify: (message: string) => void }) {
  return <Switch><Route path="/" component={() => <Dashboard state={state} setState={setState} notify={notify} />} /><Route path="/admin" component={() => <AdminOverview state={state} notify={notify} />} /><Route path="/admin/scenarios" component={() => <ScenarioManager state={state} setState={setState} notify={notify} />} /><Route path="/admin/users" component={() => <UserManager state={state} setState={setState} notify={notify} />} /><Route path="/admin/analytics" component={() => <Analytics notify={notify} />} /><Route path="/profile" component={() => <Profile state={state} notify={notify} />} /><Route path="/settings" component={() => <SettingsPage state={state} setState={setState} notify={notify} />} /><Route component={NotFound} /></Switch>;
}

function App() {
  const [state, setState] = usePersistentState();
  const { message, notify } = useToastMessage();
  return <QueryClientProvider client={queryClient}><TooltipProvider><Shell state={state} setState={setState} notify={notify}><ErrorBoundary resetKey={location.pathname}><RouterContent state={state} setState={setState} notify={notify} /></ErrorBoundary></Shell><Toast message={message} /><Toaster /></TooltipProvider></QueryClientProvider>;
}

export default App;