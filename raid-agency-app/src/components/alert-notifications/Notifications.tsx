import React, { useState } from 'react';
import {
  Badge,
  Box,
  Chip,
  Drawer,
  IconButton,
  Typography,
  Divider,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Tab,
  Tabs,
} from '@mui/material';
import {
  Notifications as NotificationsIcon,
  Close as CloseIcon,
  ExpandMore as ExpandMoreIcon,
} from '@mui/icons-material';
import { useNotificationContext } from "./notification-context/NotificationsContext";
import { useNavigate } from 'react-router';

interface NotificationBellProps {
  className?: string;
}

export const NotificationBell: React.FC<NotificationBellProps> = ({ className }) => {
  const [open, setOpen] = useState(false);
  const [topOffset, setTopOffset] = useState(0);
  const [expandedSections, setExpandedSections] = useState<{ [key: string]: boolean }>({});
  const [selectedTab, setSelectedTab] = useState<string>('');
  const { notifications, totalCount } = useNotificationContext();
  const navigate = useNavigate();

  const handleOpen = () => {
    // Measure AppBar height + Banner height (if shown in non-prod) so the drawer starts below both
    const appBar = document.querySelector<HTMLElement>('[data-testid="app-nav-bar"]');
    let offset = 0;
    if (appBar) {
      offset = appBar.getBoundingClientRect().bottom;
      const next = appBar.nextElementSibling as HTMLElement | null;
      if (next?.classList.contains('notification-banner')) {
        offset += next.getBoundingClientRect().height;
      }
    }
    setTopOffset(Math.max(0, offset));
    setOpen(true);
  };

  // Group notifications by type; untyped entries fall back to 'general'
  const groupedNotifications = Object.entries(notifications).reduce(
    (acc, [key, notification]) => {
      const typeKey = notification.type ?? 'general';
      const typeLabel = notification.typeLabel ?? 'General';
      if (!acc[typeKey]) {
        acc[typeKey] = { label: typeLabel, items: {} };
      }
      acc[typeKey].items[key] = notification;
      return acc;
    },
    {} as Record<string, { label: string; items: Record<string, typeof notifications[string]> }>,
  );

  const tabKeys = Object.keys(groupedNotifications);
  const activeTab = (selectedTab && groupedNotifications[selectedTab]) ? selectedTab : (tabKeys[0] ?? '');

  const handleAccordionChange = (key: string) => (_event: React.SyntheticEvent, isExpanded: boolean) => {
    setExpandedSections(prev => ({ ...prev, [key]: isExpanded }));
  };

  return (
    <Box className={className}>
      <IconButton onClick={handleOpen} sx={{ color: 'grey' }}>
        <Badge badgeContent={totalCount} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>

      <Drawer
        anchor="right"
        open={open}
        onClose={() => setOpen(false)}
        PaperProps={{
          sx: {
            width: { xs: '100vw', sm: 480 },
            top: `${topOffset}px`,
            height: `calc(100% - ${topOffset}px)`,
            display: 'flex',
            flexDirection: 'column',
          },
        }}
      >
        {/* Header */}
        <Box
          sx={{
            px: 3,
            py: 2.5,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: 1,
            borderColor: 'divider',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <NotificationsIcon color="primary" />
            <Box>
              <Typography variant="h6" fontWeight={700} lineHeight={1.2}>
                Notifications
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {totalCount} pending {totalCount === 1 ? 'request' : 'requests'}
              </Typography>
            </Box>
          </Box>
          <IconButton onClick={() => setOpen(false)} size="small">
            <CloseIcon />
          </IconButton>
        </Box>

        {totalCount === 0 ? (
          <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2, color: 'text.disabled' }}>
            <NotificationsIcon sx={{ fontSize: 56 }} />
            <Typography variant="body1">No notifications</Typography>
          </Box>
        ) : (
          <>
            {/* Notification type tabs */}
            <Tabs
              value={activeTab}
              onChange={(_e, v) => setSelectedTab(v)}
              variant="scrollable"
              scrollButtons="auto"
              sx={{ px: 2, borderBottom: 1, borderColor: 'divider' }}
            >
              {tabKeys.map((typeKey) => {
                const group = groupedNotifications[typeKey];
                const count = Object.values(group.items).reduce(
                  (sum, n) => sum + n.categories.length, 0,
                );
                return (
                  <Tab
                    key={typeKey}
                    value={typeKey}
                    sx={{ textTransform: 'none', minHeight: 52 }}
                    label={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="body2" fontWeight={500}>
                          {group.label}
                        </Typography>
                        <Chip
                          label={count}
                          size="small"
                          color="primary"
                          sx={{ height: 20, minWidth: 26, fontSize: '0.7rem' }}
                        />
                      </Box>
                    }
                  />
                );
              })}
            </Tabs>

            {/* Accordions for the selected tab */}
            <Box sx={{ flex: 1, overflowY: 'auto', px: 2, py: 2 }}>
              {Object.entries(groupedNotifications[activeTab]?.items ?? {}).map(([key, notification]) => (
                <Accordion
                  disableGutters
                  key={key}
                  expanded={expandedSections[key] === true}
                  onChange={handleAccordionChange(key)}
                  elevation={0}
                  sx={{
                    mb: 1.5,
                    border: 1,
                    borderColor: 'divider',
                    borderRadius: '8px !important',
                    '&:before': { display: 'none' },
                    '&.Mui-expanded': { borderColor: 'primary.main' },
                  }}
                >
                  <AccordionSummary
                    expandIcon={<ExpandMoreIcon />}
                    sx={{ px: 2, py: 0.5, borderRadius: 2 }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, width: '100%', pr: 1 }}>
                      <Typography variant="subtitle2" fontWeight={600} sx={{ flex: 1 }}>
                        {notification.title}
                      </Typography>
                      <Chip
                        label={`${notification.categories.length} pending`}
                        size="small"
                        variant="outlined"
                        color="default"
                        sx={{ fontSize: '0.7rem' }}
                      />
                    </Box>
                  </AccordionSummary>

                  <AccordionDetails sx={{ p: 0 }}>
                    <Divider />
                    <List disablePadding>
                      {notification.categories.map((category, index) => (
                        <React.Fragment key={index}>
                          <ListItem
                            sx={{
                              px: 2,
                              py: 1.5,
                              gap: 1,
                              '&:hover': { bgcolor: 'action.hover' },
                            }}
                          >
                            <ListItemIcon sx={{ minWidth: 44 }}>
                              <Box
                                sx={{
                                  width: 36,
                                  height: 36,
                                  borderRadius: '50%',
                                  bgcolor: 'primary.50',
                                  color: 'primary.main',
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                }}
                              >
                                {category.titleIcon}
                              </Box>
                            </ListItemIcon>
                            <ListItemText
                              primary={
                                <Typography variant="body2" fontWeight={600}>
                                  {category.name}
                                </Typography>
                              }
                              secondary={
                                category.email
                                  ? <Typography variant="caption" color="text.secondary">{category.email}</Typography>
                                  : undefined
                              }
                            />
                            {category.actions && (
                              <Box sx={{ display: 'flex', gap: 0.75, flexShrink: 0 }}>
                                {category.actions}
                              </Box>
                            )}
                            {category.button && (
                              <Box sx={{ flexShrink: 0 }}>
                                {category.button}
                              </Box>
                            )}
                          </ListItem>
                          {index < notification.categories.length - 1 && (
                            <Divider variant="inset" component="li" />
                          )}
                        </React.Fragment>
                      ))}
                    </List>
                  </AccordionDetails>
                </Accordion>
              ))}
            </Box>

            {/* Footer */}
            <Box
              sx={{
                px: 3,
                py: 2,
                borderTop: 1,
                borderColor: 'divider',
                textAlign: 'center',
              }}
            >
              <Typography
                variant="body2"
                color="primary"
                sx={{ cursor: 'pointer', fontWeight: 500 }}
                onClick={() => { setOpen(false); navigate("/service-points"); }}
              >
                View all in Service Points
              </Typography>
            </Box>
          </>
        )}
      </Drawer>
    </Box>
  );
};
