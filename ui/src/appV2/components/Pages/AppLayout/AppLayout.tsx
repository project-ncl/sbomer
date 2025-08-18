import { IAppRoute, IAppRouteGroup, routes } from '@appV2/routes';
import {
  Application,
  Asleep,
  ChevronRight,
  Dashboard,
  DocumentMultiple_02,
  EventChange,
  Switcher as SwitcherIcon
} from '@carbon/icons-react';
import {
  Switcher as CarbonSwitcher,
  Column,
  Content,
  Grid,
  Header,
  HeaderContainer,
  HeaderGlobalAction,
  HeaderGlobalBar,
  HeaderMenuButton,
  HeaderName,
  HeaderPanel,
  SideNav,
  SideNavHeader,
  SideNavItems,
  SideNavLink,
  SwitcherItem,
  Theme
} from '@carbon/react';
import * as React from 'react';
import { NavLink, useLocation } from 'react-router-dom';

interface IAppLayout {
  children: React.ReactNode;
}

const AppLayout: React.FunctionComponent<IAppLayout> = ({ children }) => {
  const location = useLocation();
  const [sideNavExpanded, setSideNavExpanded] = React.useState(true);
  const [menuPanelExpanded, setMenuPanelExpanded] = React.useState(false);

  const [currentTheme, setCurrentTheme] = React.useState<'white' | 'g100'>(() => {
    const savedTheme = localStorage.getItem('sbomer-theme');
    return (savedTheme as 'white' | 'g100') || 'g100';
  });

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
  const headerContainer = <>
    <HeaderContainer
      render={({ onClickSideNavExpand }) => (
        <>
          <Header aria-label="SBOMER">
            <HeaderMenuButton
              aria-label={sideNavExpanded ? 'Close menu' : 'Open menu'}
              onClick={() => {
                setSideNavExpanded(!sideNavExpanded);
                onClickSideNavExpand();
              }}
              isActive={sideNavExpanded}
              isCollapsible={true}
            />
            <HeaderName prefix=''>
              SBOMer
            </HeaderName>

            <HeaderGlobalBar>
              <HeaderGlobalAction
                aria-label="Theme switcher"
                onClick={() => {
                  setCurrentTheme(prevTheme => {
                    const newTheme = prevTheme === 'g100' ? 'white' : 'g100';
                    localStorage.setItem('sbomer-theme', newTheme);
                    return newTheme as 'white' | 'g100';
                  });
                }}
              >
                <Asleep size={20} />
              </HeaderGlobalAction>
              <HeaderGlobalAction
                aria-label="App switcher"
                isActive={menuPanelExpanded}
                onClick={() => setMenuPanelExpanded(!menuPanelExpanded)}
                tooltipAlignment="end"
              >
                <SwitcherIcon size={20} />
              </HeaderGlobalAction>
            </HeaderGlobalBar>

          </Header>

          <HeaderPanel
            aria-label="Application Switcher"
            expanded={menuPanelExpanded}
          >
            <CarbonSwitcher aria-label="Switcher Container">
              <SwitcherItem
                aria-label="Switch to SBOMer Classic"
                href="/"
                onClick={() => {
                  if (window.location.pathname !== '/') {
                    window.location.href = '/';
                  }
                  setMenuPanelExpanded(false);
                }}
              >
                Switch to SBOMer Classic
              </SwitcherItem>
            </CarbonSwitcher>
          </HeaderPanel>
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
  </>

  const content = <Content id="main-content">
    <Grid>
      <Column sm={4} md={8} lg={16}>
        {children}
      </Column>
    </Grid>
  </Content>;


  return (
    <Theme theme={currentTheme}>
      {headerContainer}
      {content}
    </Theme>
  );
};

export { AppLayout };
