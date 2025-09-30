import { IAppRoute, IAppRouteGroup, routes } from '@appV2/routes';
import {
  Application,
  ChevronRight,
  Dashboard,
  DocumentMultiple_02,
  EventChange,
  Help,
  Switcher as SwitcherIcon
} from '@carbon/icons-react';
import {
  Switcher as CarbonSwitcher,
  Column,
  ContainedList,
  ContainedListItem,
  Content,
  Grid,
  Header,
  HeaderContainer,
  HeaderGlobalAction,
  HeaderGlobalBar,
  HeaderMenuButton,
  HeaderName,
  HeaderPanel,
  Layer,
  Select,
  SelectItem,
  SideNav,
  SideNavDivider,
  SideNavItems,
  SideNavLink,
  SideNavMenu,
  SideNavMenuItem,
  SkipToContent,
  Theme
} from '@carbon/react';
import * as React from 'react';
import { NavLink, useLocation } from 'react-router-dom';

interface IAppLayout {
  children: React.ReactNode;
}

const AppLayout: React.FunctionComponent<IAppLayout> = ({ children }) => {
  const location = useLocation();
  // enables the sidebar to be expanded by default instead of using the sidenav prop
  const [menuPanelExpanded, setMenuPanelExpanded] = React.useState(false);

  const [currentTheme, setCurrentTheme] = React.useState<'white' | 'g10' | 'g90' | 'g100'>(() => {
    const savedTheme = localStorage.getItem('sbomer-theme');
    if (savedTheme === 'white' || savedTheme === 'g10' || savedTheme === 'g90' || savedTheme === 'g100') {
      return savedTheme;
    }
    // default to system preference on first load
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    return prefersDark ? 'g100' : 'white';
  });

  const getRouteIcon = (path: string, label?: string) => {
    if (path === '/' || label === 'Dashboard') return Dashboard;
    if (path.includes('/generations') || label === 'Generations') return Application;
    if (path.includes('/manifests') || label === 'Manifests') return DocumentMultiple_02;
    if (path.includes('/events') || label === 'Events') return EventChange;
    if (path.includes('/help') || label === 'Help') return Help;
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

  const renderSideNavGroup = (group: IAppRouteGroup, groupIndex: number) => {
    const isAnyChildActive = group.routes?.some(r => r.path && isRouteActive(r.path));
    return (
      <><SideNavMenu
        key={`${group.label}-${groupIndex}`}
        title={group.label}
        defaultExpanded={isAnyChildActive}
      >
        {group.routes.map((route, idx) => route.label ? (
          <SideNavMenuItem
            key={`${route.label}-${idx}`}
            as={NavLink}
            to={route.path}
          >
            {route.label}
          </SideNavMenuItem>
        ) : null
        )}
      </SideNavMenu>
        <SideNavDivider />
      </>
    );
  };

  const Navigation = (
    <SideNavItems>
      {routes
        .filter(route => route.label && route.label !== 'Help')
        .map((route, idx) =>
          !route.routes
            ? renderSideNavLink(route, idx)
            : renderSideNavGroup(route, idx)
        )}

      <SideNavDivider />
      <SideNavLink
        key="help-link"
        renderIcon={Help}
        isActive={isRouteActive('/help')}
        as={NavLink}
        to="/help"
        large
      >
        Help
      </SideNavLink>
    </SideNavItems>
  );

  return (
    <Theme theme={currentTheme}>
      <div className='page-wrapper'>
        <HeaderContainer isSideNavExpanded={true} render={({ isSideNavExpanded, onClickSideNavExpand }) => (
          <>
            <SkipToContent href="#main-content">Skip to content</SkipToContent>
            <Header aria-label="SBOMER">
              <HeaderMenuButton
                aria-label={isSideNavExpanded ? 'Close menu' : 'Open menu'}
                onClick={() => onClickSideNavExpand()}
                isActive={isSideNavExpanded}
                isCollapsible={true}
              />
              <HeaderName prefix="" href="/nextgen">SBOMer</HeaderName>

              <HeaderGlobalBar>
                <HeaderGlobalAction
                  aria-label="Options"
                  isActive={menuPanelExpanded}
                  onClick={() => setMenuPanelExpanded(!menuPanelExpanded)}
                  tooltipAlignment="end"
                >
                  <SwitcherIcon size={20} />
                </HeaderGlobalAction>
              </HeaderGlobalBar>

            </Header>

            <HeaderPanel aria-label="Application Switcher" expanded={menuPanelExpanded}>
              <Layer>
                <CarbonSwitcher aria-label="Switcher Container">
                  <ContainedList label="Navigation" kind="on-page">
                    <ContainedListItem
                      onClick={() => {
                        window.location.href = '/';
                        setMenuPanelExpanded(false);
                      }}
                    >
                      Switch to SBOMer Classic
                    </ContainedListItem>
                  </ContainedList>

                  <ContainedList label="Appearance" kind="on-page">
                    <ContainedListItem>
                      <Select
                        id="theme-select"
                        labelText="Theme"
                        hideLabel
                        size="sm"
                        value={currentTheme}
                        onChange={(e) => {
                          const newTheme = e.target.value as 'white' | 'g10' | 'g90' | 'g100';
                          setCurrentTheme(newTheme);
                          localStorage.setItem('sbomer-theme', newTheme);
                        }}
                      >
                        <SelectItem value="white" text="White (Light)" />
                        <SelectItem value="g10" text="Gray 10 (Light)" />
                        <SelectItem value="g90" text="Gray 90 (Dark)" />
                        <SelectItem value="g100" text="Gray 100 (Darkest)" />
                      </Select>
                    </ContainedListItem>
                  </ContainedList>
                </CarbonSwitcher>
              </Layer>
            </HeaderPanel>
            <SideNav
              aria-label="Side navigation"
              expanded={isSideNavExpanded}
              isFixedNav
              isPersistent
              isRail
              isChildOfHeader
            >
              {Navigation}
            </SideNav>

          </>
        )} />
        <Content className="main-content">
          <Grid>
            <Column sm={4} md={8} lg={16}>
              {children}
            </Column>
          </Grid>
        </Content>
      </div>
    </Theme>
  );
};

export { AppLayout };
