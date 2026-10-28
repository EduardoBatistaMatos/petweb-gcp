import { Link, NavLink, Outlet } from 'react-router-dom';
import logo from '../assets/logo.jpg';

export default function AppLayout() {
  return <div className="app-layout">
    <header className="app-header">
      <div className="app-header-inner">
        <Link className="brand" to="/pets" aria-label="Petshop, ir para a lista de pets">
          <span className="brand-mark" aria-hidden="true"><img className="brand-logo" src={logo} alt="" /></span>
          <span className="brand-copy"><strong>PETWEB</strong><small>Gestão com carinho</small></span>
        </Link>
        <nav className="main-nav" aria-label="Navegação principal">
          <NavLink to="/pets" end className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}>Pets</NavLink>
          <Link to="/pets/novo" className="button button-primary nav-create"><span aria-hidden="true">＋</span><span>Novo pet</span></Link>
        </nav>
      </div>
    </header>
    <Outlet />
  </div>;
}
