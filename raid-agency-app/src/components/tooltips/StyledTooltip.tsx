
import React from 'react';
import {
  Tooltip,
  TooltipProps,
  Typography,
  IconButton,
  styled,
  tooltipClasses
} from '@mui/material';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';

interface CustomTooltipProps {
  title: string;
  content: string | React.ReactNode;
  learnMoreUrl?: string;
  variant?: 'info' | 'warning' | 'error' | 'success';
  placement?: TooltipProps['placement'];
  children?: React.ReactElement;
  tooltipIcon?: React.ReactElement;
}

const HtmlTooltip = styled(({ className, ...props }: TooltipProps) => (
  <Tooltip {...props} classes={{ popper: className }} />
))(({ theme }) => ({
  [`& .${tooltipClasses.tooltip}`]: {
    backgroundColor: '#ffffff',
    color: 'rgba(0, 0, 0, 0.87)',
    maxWidth: 300,
    fontSize: theme.typography.pxToRem(12),
    border: '1px solid #dadde9',
    borderRadius: theme.shape.borderRadius,
    padding: theme.spacing(2),
    boxShadow: theme.shadows[3],
  },
  [`& .${tooltipClasses.arrow}`]: {
    color: '#ffffff',
    '&::before': {
      backgroundColor: '#ffffff',
      border: '1px solid #dadde9',
    },
  },
}));

export const CustomStyledTooltip: React.FC<CustomTooltipProps> = ({
  title,
  content,
  placement,
  tooltipIcon = <HelpOutlineIcon />,
}) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const tooltipRef = React.useRef<HTMLDivElement>(null);
  const buttonRef = React.useRef<HTMLButtonElement>(null);
  const handleClickOutside = (event: MouseEvent): void => {
      const target = event.target as Node;
      if (tooltipRef.current && !tooltipRef.current.contains(target) &&
          buttonRef.current && !buttonRef.current.contains(target)) {
        setIsOpen(false);
      }
    };

    React.useEffect(() => {
      if (isOpen) {
        document.addEventListener('mousedown', handleClickOutside);
      } else {
        document.removeEventListener('mousedown', handleClickOutside);
      }
  
      return (): void => {
        document.removeEventListener('mousedown', handleClickOutside);
      };
    }, [isOpen]);

  const customContent = (
    <React.Fragment>
      <Typography color="primary" variant="subtitle1" sx={{ fontWeight: 600 }}>
        {title}
      </Typography>
      <Typography variant="body2" sx={{ mt: 1 }}>
        {content}
      </Typography>
    </React.Fragment>
  );

  return (
    <HtmlTooltip
      title={customContent}
      arrow={true}
      placement={placement || 'top'}
      open={isOpen}
    >
      <IconButton
        id="tooltip-button"
        color="primary"
        onClick={() => setIsOpen(!isOpen)}
        onMouseEnter={() => setIsOpen(true)}
        onMouseLeave={() => setIsOpen(false)}
        ref={buttonRef}
      >
        <div style={{ all: 'inherit', padding: 0 }} ref={tooltipRef}>
          {tooltipIcon}
        </div>
      </IconButton>
    </HtmlTooltip>
  );
};
