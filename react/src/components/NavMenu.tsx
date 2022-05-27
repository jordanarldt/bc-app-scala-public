import React from "react";
import { NavLink } from "react-router-dom";
import styled from "styled-components";
import { AppRoute } from "../types";

interface NavMenuProps {
  routes: AppRoute[];
}

interface NavMenuItemProps {
  route: AppRoute;
}

export const NavMenu = ({ routes }: NavMenuProps) => {
  return (
    <Container>
      {routes.map((route, index) => (
        <NavMenuItem key={`nav-menu-item-${index}`} route={route} />
      ))}
    </Container>
  );
};

const NavMenuItem = ({ route }: NavMenuItemProps) => {
  return (
    <Item>
      <MenuLink to={route.path}>{route.icon}{route.name}</MenuLink>
    </Item>
  );
};

const Container = styled.ul`
  padding-left: 0;
`;

const Item = styled.li`
  display: inline-block;
  margin: 0 0.5em;
`;

const MenuLink = styled(NavLink)`
  display: block;
  font-weight: bold;
  padding: 0.5em 1em;
  border-radius: 0.75em;
  text-decoration: none;
  transition: 100ms;
  background-color: ${({ theme }) => theme.colors.primary20};

  &.active {
    color: white;
    background-color: ${({ theme }) => theme.colors.primary};
    box-shadow: 0px 4px 8px ${({ theme }) => theme.colors.primary30};
  }

  &:not(.active):hover {
    color: ${({ theme }) => theme.colors.primary30};
    background-color: ${({ theme }) => theme.colors.primary20};
  }

  & svg {
    margin-right: 0.25em;
  }
`;
