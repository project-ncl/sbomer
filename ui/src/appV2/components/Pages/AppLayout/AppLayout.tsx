import {
  Button,
  Content,
  Header,
  HeaderContainer,
  HeaderMenuButton,
  HeaderName,
  SideNav,
  SideNavHeader,
  SideNavItems,
  SideNavLink,
} from '@carbon/react';
import rhlogo from '../../../../assets/Logo-Red_Hat-A-Standard-RGB.svg';
import { IAppRoute, IAppRouteGroup, routes } from '@appV2/routes';
import * as React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  Dashboard,
  Application,
  DocumentMultiple_02,
  Events,
  ChevronRight,
  EventChange
} from '@carbon/icons-react';

interface IAppLayout {
  children: React.ReactNode;
}

const AppLayout: React.FunctionComponent<IAppLayout> = ({ children }) => {
  const location = useLocation();
  const [sideNavExpanded, setSideNavExpanded] = React.useState(true);

  const getRouteIcon = (path: string, label?: string) => {
    if (path === '/' || label === 'Dashboard') return Dashboard;
    if (path.includes('/generations') || label === 'Generations') return Application;
    if (path.includes('/manifests') || label === 'Manifests') return DocumentMultiple_02;
    if (path.includes('/events') || label === 'Events') return EventChange;
    return ChevronRight;
  };

  const isRouteActive = (routePath: string) => {
    if (routePath === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(routePath);
  };

  const renderSideNavLink = (route: IAppRoute, index: number) => (
    <SideNavLink
      key={`${route.label}-${index}`}
      renderIcon={getRouteIcon(route.path, route.label)}
      isActive={isRouteActive(route.path)}
      as={NavLink}
      to={route.path}
      large
    >
      {route.label}
    </SideNavLink>
  );

  const renderSideNavGroup = (group: IAppRouteGroup, groupIndex: number) => (
    <React.Fragment key={`${group.label}-${groupIndex}`}>
      <SideNavHeader renderIcon={ChevronRight}>{group.label}</SideNavHeader>
      {group.routes.map((route, idx) => route.label && renderSideNavLink(route, idx))}
    </React.Fragment>
  );

  const Navigation = (
    <SideNavItems>
      {routes
        .filter(route => route.label)
        .map((route, idx) =>
          !route.routes
            ? renderSideNavLink(route, idx)
            : renderSideNavGroup(route, idx)
        )}
    </SideNavItems>
  );

  return (
    <>
    <HeaderContainer
      render={({ onClickSideNavExpand }) => (
        <>
          <Header aria-label="SBOMER NEXT GEN">
            <HeaderMenuButton
              aria-label={sideNavExpanded ? 'Close menu' : 'Open menu'}
              onClick={() => {
                setSideNavExpanded(!sideNavExpanded);
                onClickSideNavExpand();
              }}
              isActive={sideNavExpanded}
              isCollapsible={true}
            />
            <HeaderName prefix="">
              <img src={rhlogo} alt="Red Hat Logo" height={32} style={{ marginRight: '0.5rem' }} />
              <h3>SBOMER NEXT GEN</h3>
            </HeaderName>

            <div style={{ marginLeft: 'auto' }}>
              <Button
                kind="primary"
                size="sm"
                onClick={() => window.location.pathname !== '/' && (window.location.href = '/')}
              >
                Go to Classic
              </Button>
            </div>
          </Header>
          <SideNav
            aria-label="Side navigation"
            expanded={sideNavExpanded}
            isFixedNav
            isPersistent
            isRail
            isChildOfHeader
          >
            {Navigation}
          </SideNav>
          
        </>
      )}
    />
    <Content id="main-content" style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            minHeight: 'calc(100vh - 48px)',
            flex: 1 
          }}>
            {children}
          </Content>
          </>
    
  );
};

export { AppLayout };
