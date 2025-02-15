import { useKeycloak } from "@/contexts/keycloak-context";
import { useAuthHelper } from "@/keycloak";
import {
  Add as AddIcon,
  ExitToApp as ExitToAppIcon,
  GroupWork as GroupWorkIcon,
  Home as HomeIcon,
  Hub as HubIcon,
  Key as KeyIcon,
  ListAltOutlined as ListAltOutlinedIcon,
  Menu as MenuIcon,
  Layers as LayersIcon,
} from "@mui/icons-material";
import {
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Tooltip,
} from "@mui/material";
import React from "react";
import { Link, NavLink } from "react-router-dom";

function CombinedMenu() {
  const { isOperator, isGroupAdmin } = useAuthHelper();
  const { logout } = useKeycloak();

  const menuItems = [
    {
      label: "Home",
      link: "/",
      icon: <HomeIcon />,
      isNavLink: true,
    },
    {
      label: "Mint new RAiD",
      link: "/raids/new",
      icon: <AddIcon />,
      isNavLink: true,
    },
    {
      label: "Show all RAiDs",
      link: "/raids",
      icon: <ListAltOutlinedIcon />,
      isNavLink: true,
    },
    {
      label: "Manage service points",
      link: "/service-points",
      icon: <HubIcon />,
      hidden: !isOperator && !isGroupAdmin,
      isNavLink: true,
    },
    {
      label: "Create API key",
      link: "/api-key",
      icon: <KeyIcon />,
      isNavLink: true,
    },
    {
      label: "Your received invites",
      link: "/invites",
      icon: <GroupWorkIcon />,
      isNavLink: false,
    },
    {
      label: "Cache Manager",
      link: "/cache-manager",
      icon: <LayersIcon />,
      isNavLink: false,
    },
    {
      label: "Sign out",
      icon: <ExitToAppIcon />,
      onClick: () => {
        localStorage.removeItem("client_id");
        logout();
      },
    },
  ];

  return (
    <List>
      {menuItems.map((item) => (
        <Tooltip
          placement="top"
          title={item.hidden ? "Not enabled for your user" : ""}
          key={item.link || item.label}
        >
          <ListItem disablePadding>
            {item.onClick ? (
              <ListItemButton onClick={item.onClick} disabled={item.hidden}>
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ) : item.isNavLink ? (
              <ListItemButton
                component={NavLink}
                to={item.link || ""}
                disabled={item.hidden}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ) : (
              <ListItemButton
                component={Link}
                to={item.link || ""}
                disabled={item.hidden}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            )}
          </ListItem>
        </Tooltip>
      ))}
    </List>
  );
}

export default function NavigationDrawer() {
  const [drawerOpen, setDrawerOpen] = React.useState(false);

  const toggleDrawer = (newOpen: boolean) => () => {
    setDrawerOpen(newOpen);
  };

  return (
    <>
      <IconButton
        size="large"
        edge="start"
        aria-label="toggle drawer"
        onClick={toggleDrawer(true)}
      >
        <MenuIcon />
      </IconButton>
      <Drawer
        anchor="right"
        open={drawerOpen}
        onClose={toggleDrawer(false)}
        sx={{
          flexShrink: 0,
          "& .MuiDrawer-paper": {
            width: 320,
            boxSizing: "border-box",
          },
        }}
      >
        <Toolbar />
        <CombinedMenu />
      </Drawer>
    </>
  );
}
