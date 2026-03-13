import React, { useState } from 'react';
import {
  Badge,
  IconButton,
  Popper,
  Paper,
  ClickAwayListener,
  Box,
  Typography,
  Divider,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Chip,
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
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [expandedSections, setExpandedSections] = useState<{ [key: string]: boolean }>({});
  const { notifications, totalCount } = useNotificationContext();
  const open = Boolean(anchorEl);
  const navigate = useNavigate();
  const handleClick = (event: React.MouseEvent<HTMLElement>): void => {
    setAnchorEl(anchorEl ? null : event.currentTarget);
  };

  const handleClose = (): void => {
    setAnchorEl(null);
  };

  const handleAccordionChange = (key: string) => (event: React.SyntheticEvent, isExpanded: boolean) => {
    setExpandedSections(prev => ({
      ...prev,
      [key]: isExpanded,
    }));
  };

  return (
    <Box className={className}>
      <IconButton onClick={handleClick} sx={{ position: 'relative', color: 'grey' }}>
        <Badge badgeContent={totalCount} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>

      <Popper 
        open={open} 
        anchorEl={anchorEl} 
        placement="bottom-end" 
        sx={{ zIndex: 1300 }}
      >
        <ClickAwayListener onClickAway={handleClose}>
          <Paper
            elevation={8}
            sx={{
              width: 420,
              maxWidth: '90vw',
              maxHeight: 600,
              mt: 1,
              borderRadius: 2,
              overflow: 'hidden',
              display: 'flex',
              flexDirection: 'column',
            }}
          >
            {/* Header */}
            <Box
              sx={{
                p: 2,
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                color: 'white',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <Box>
                <Typography variant="h6" fontWeight={700}>
                  Notifications
                </Typography>
                <Typography variant="caption" sx={{ opacity: 0.9 }}>
                  {totalCount} total notifications
                </Typography>
              </Box>
              <IconButton size="small" onClick={handleClose} sx={{ color: 'white' }}>
                <CloseIcon fontSize="small" />
              </IconButton>
            </Box>

            {/* Content */}
            <Box sx={{ flex: 1, overflow: 'auto', bgcolor: 'background.default' }}>
              {totalCount === 0 ? (
                <Box sx={{ p: 8, textAlign: 'center' }}>
                  <NotificationsIcon sx={{ fontSize: 64, color: 'action.disabled', mb: 2 }} />
                  <Typography color="text.secondary">No notifications</Typography>
                </Box>
              ) : (
                <Box sx={{ p: 1 }}>
                  {Object.entries(notifications).map(([key, notification]) => (
                    <Accordion
                      disableGutters={true}
                      key={key}
                      expanded={expandedSections[key] !== false}
                      onChange={handleAccordionChange(key)}
                      sx={{
                        mb: 1,
                        '&:before': { display: 'none' },
                        boxShadow: 1,
                      }}
                    >
                      <AccordionSummary
                        expandIcon={<ExpandMoreIcon />}
                        sx={{
                          borderLeft: `${expandedSections[key] ? 4 : 0}px solid`,
                          borderColor: 'primary.main',
                          '&:hover': { bgcolor: 'action.hover' },
                        }}
                      >
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, width: '100%' }}>
                          <Typography variant="subtitle1" fontWeight={600} sx={{ flex: 1 }}>
                            {notification.title}
                          </Typography>
                          <Chip
                            label={notification.categories.length}
                            size="small"
                            color="primary"
                            sx={{ minWidth: 25 }}
                          />
                        </Box>
                      </AccordionSummary>
                      <AccordionDetails sx={{ p: 0, bgcolor: 'background.default' }}>
                        <List
                          sx={{ 
                            p: 1,
                            maxHeight: 200, // Set maximum height (adjust as needed)
                            overflowY: 'auto', // Enable vertical scrolling
                            '&::-webkit-scrollbar': {
                              width: '8px',
                            },
                            '&::-webkit-scrollbar-track': {
                              backgroundColor: 'background.default',
                              borderRadius: '4px',
                            },
                            '&::-webkit-scrollbar-thumb': {
                              backgroundColor: 'action.hover',
                              borderRadius: '4px',
                              '&:hover': {
                                backgroundColor: 'action.selected',
                              },
                            },
                          }}
                        >
                          {notification.categories.map((category, index) => (
                            <ListItem
                              key={index}
                              sx={{
                                bgcolor: 'background.paper',
                                mb: 1,
                                borderRadius: 1,
                                border: 0,
                                borderColor: 'divider',
                                '&:hover': {
                                  boxShadow: 2,
                                },
                                '&:last-child': {
                                  mb: 0,
                                },
                              }}
                            >
                              <ListItemIcon sx={{ minWidth: 48 }}>
                                <Box
                                  sx={{
                                    width: 40,
                                    height: 40,
                                    borderRadius: '50%',
                                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                    color: 'white',
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
                                  <Typography variant="body1" fontWeight={600} sx={{ wordBreak: 'break-word' }}>
                                    {category.name}
                                  </Typography>
                                }
                              />
                              {category.actions && (
                                <Box sx={{ display: 'flex', gap: 0.5, ml: 1 }}>
                                  {category.actions}
                                </Box>
                              )}
                              {category.button && (
                                <Box sx={{ ml: 1 }}>
                                  {category.button}
                                </Box>
                              )}
                              <Divider />
                            </ListItem>
                          ))}
                        </List>
                      </AccordionDetails>
                    </Accordion>
                  ))}
                </Box>
              )}
            </Box>

            {/* Footer (Optional) */}
            {totalCount > 0 && (
              <>
                <Box sx={{ p: 1.5, textAlign: 'center', bgcolor: 'background.paper' }}>
                  <Typography variant="body2" color="primary" sx={{ cursor: 'pointer' }} onClick={() => {navigate("/service-points")}}>
                    View All Notifications
                  </Typography>
                </Box>
              </>
            )}
          </Paper>
        </ClickAwayListener>
      </Popper>
    </Box>
  );
};
