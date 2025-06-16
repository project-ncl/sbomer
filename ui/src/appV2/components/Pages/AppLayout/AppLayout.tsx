import rhlogo from '../../../../assets/Logo-Red_Hat-A-Standard-RGB.svg';
import { IAppRoute, IAppRouteGroup, routes } from '@appV2/routes';
import {
  Brand,
  Button,
  Masthead,
  MastheadLogo,
  MastheadMain,
  MastheadToggle, MastheadBrand,
  Nav,
  NavExpandable,
  NavItem,
  NavList,
  Page,
  PageSidebar,
  PageSidebarBody,
  SkipToContent,
  Flex,
  MastheadContent,
  FlexItem,
} from '@patternfly/react-core';
import { BarsIcon } from '@patternfly/react-icons';
import * as React from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';

interface IAppLayout {
  children: React.ReactNode;
}

const AppLayout: React.FunctionComponent<IAppLayout> = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = React.useState(true);

  // Bar above the app with the button
  const TopBar = (
    <div
      style={{
        width: '100%',
        background: '#2196f3',
        display: 'flex',
        justifyContent: 'flex-end',
        alignItems: 'center',
        padding: '0.5rem 1rem',
        boxSizing: 'border-box',
        zIndex: 2000,
      }}
    >
      <span style={{ color: '#fff', fontWeight: 'bold', fontSize: '1.1rem', letterSpacing: '1px', marginRight: '1rem' }}>
        SBOMER NEXT GEN
      </span>
      <Button
        variant="primary"
        style={{ color: '#2196f3', backgroundColor: '#fff', borderColor: '#fff' }}
        onClick={() => window.location.pathname !== '/' && (window.location.href = '/')}
      >
        Go to Classic
      </Button>
    </div>
  );

  const Header = (
    <Masthead>
      <MastheadMain>
        <MastheadToggle>
          <Button
            icon={<BarsIcon />}
            variant="plain"
            onClick={() => setSidebarOpen(!sidebarOpen)}
            aria-label="Global navigation"
          />
        </MastheadToggle>
        <MastheadBrand data-codemods>
          <MastheadLogo data-codemods component="a">
            <Link to="/">
              <Brand src={rhlogo} alt="Red Hat" widths={{ default: '150px' }} />
            </Link>
          </MastheadLogo>
        </MastheadBrand>
      </MastheadMain>
    </Masthead>
  );

  const location = useLocation();

  const renderNavItem = (route: IAppRoute, index: number) => (
    <NavItem key={`${route.label}-${index}`} id={`${route.label}-${index}`} isActive={route.path === location.pathname}>
      <NavLink to={route.path}>
        {route.label}
      </NavLink>
    </NavItem>
  );

  const renderNavGroup = (group: IAppRouteGroup, groupIndex: number) => (
    <NavExpandable
      key={`${group.label}-${groupIndex}`}
      id={`${group.label}-${groupIndex}`}
      title={group.label}
      isActive={group.routes.some((route) => route.path === location.pathname)}
    >
      {group.routes.map((route, idx) => route.label && renderNavItem(route, idx))}
    </NavExpandable>
  );

  const Navigation = (
    <Nav id="nav-primary-simple">
      <NavList id="nav-list-simple">
        {routes.map(
          (route, idx) => route.label && (!route.routes ? renderNavItem(route, idx) : renderNavGroup(route, idx)),
        )}
      </NavList>
    </Nav>
  );

  const Sidebar = (
    <PageSidebar>
      <PageSidebarBody>{Navigation}</PageSidebarBody>
    </PageSidebar>
  );

  const pageId = 'primary-app-container';

  const PageSkipToContent = (
    <SkipToContent
      onClick={(event) => {
        event.preventDefault();
        const primaryContentContainer = document.getElementById(pageId);
        primaryContentContainer && primaryContentContainer.focus();
      }}
      href={`#${pageId}`}
    >
      Skip to Content
    </SkipToContent>
  );

  return (
    <>
      {TopBar}
      <Page mainContainerId={pageId} masthead={Header} sidebar={sidebarOpen && Sidebar} skipToContent={PageSkipToContent}>
        {children}
      </Page>
    </>
  );
};

export { AppLayout };
