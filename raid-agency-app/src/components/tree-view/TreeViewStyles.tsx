import { styled, alpha } from '@mui/material/styles';
import { TreeItem, treeItemClasses, TreeItemProps } from '@mui/x-tree-view/TreeItem'

/* 1. Define the styled component that holds all the visual customizations */
export const BorderedTreeItem = styled(TreeItem, {
  shouldForwardProp: (prop) => prop !== 'data-iscustomparent' && prop !== 'data-isLastChild',
})<TreeItemProps & { 'data-iscustomparent'?: 'true' | 'false'; 'data-isLastChild'?: 'true' | 'false' }>(({ theme, ...props }) => ({
  color: theme.palette.grey[200],

  // Base content styles for all nodes
  [`& .${treeItemClasses.content}`]: {
    borderRadius: theme.spacing(0.5),
    padding: theme.spacing(0.5, 1),
    margin: theme.spacing(0.2, 0),
    position: 'relative',
    color: alpha(theme.palette.primary.main, 0.9),
    [`& .${treeItemClasses.label}`]: {
      fontSize: '0.8rem',
      fontWeight: 500,
    },

    // Horizontal connector segment: ONLY on leaf nodes (data-iscustomparent='false')
    ...(props['data-iscustomparent'] === 'false' && {
      '&::before': {
        content: '""',
        position: 'absolute',
        top: '50%',
        left: '-18px',
        width: '20px',
        height: '1px',
        backgroundColor: alpha(theme.palette.text.primary, 0.6),
        transform: 'translateY(-50%)',
        zIndex: 1,
      },
    }),
  },

  // Custom styling for the icon container: ONLY on parent nodes (data-iscustomparent='true')
  ...(props['data-iscustomparent'] === 'true' && {
    [`& .${treeItemClasses.iconContainer}`]: {
      borderRadius: '50%',
      padding: theme.spacing(0, 1.2),
      ...theme.applyStyles('dark', {
        color: theme.palette.primary.contrastText,
      }),
    },
    [`& .${treeItemClasses.checkbox}`]: {
      color: alpha(theme.palette.primary.main, 0.9),
    },
  }),
  // Icon container styling for LEAF nodes (data-iscustomparent='false'): HIDE IT
  ...(props['data-isLastChild'] === 'true' && {
    [`& .${treeItemClasses.iconContainer}`]: {
      display: 'none',
    },
    [`& .${treeItemClasses.label}`]: {
      color: theme.palette.grey[900],
    },
    [`& .${treeItemClasses.checkbox}`]: {
      color: alpha(theme.palette.grey[900], 0.70),
    },
  }),
  // Vertical connection border for the children group (independent of node type)
  [`& .${treeItemClasses.groupTransition}`]: {
    marginLeft: 17,
    paddingLeft: 18,
    borderLeft: `1px solid ${alpha(theme.palette.text.primary, 0.6)}`,
  },
  // Theme-specific color for all nodes
  ...theme.applyStyles('light', {
    color: theme.palette.grey[800],
  }),
}));
