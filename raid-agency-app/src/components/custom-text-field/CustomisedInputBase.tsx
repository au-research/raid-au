import * as React from 'react';
import Paper from '@mui/material/Paper';
import InputBase from '@mui/material/InputBase';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';

export default function CustomizedInputBase({
    placeholder,
    startEdorment,
    endEdorment,
    value,
    searchValue,
    optionalIcon,
    divider = false,
}: {
    placeholder?: string,
    startEdorment?: React.ReactNode,
    endEdorment?: React.ReactNode,
    value?: string,
    searchValue?: (value: string) => void,
    optionalIcon?: React.ReactNode,
    divider?: boolean,
}) {
    const [inputValue, setInputValue] = React.useState('');
    const handleSearch = (e?: React.SyntheticEvent) => {
        e?.preventDefault();
        searchValue?.((inputValue || value) || '');
    }
  return (
    <Paper
      sx={{ p: '2px 4px', display: 'flex', alignItems: 'center', width: { xs: '100%', sm: '100%' }, height: 56, boxShadow: 'none',
      border: (theme) =>
      `1px solid ${
        theme.palette.mode === 'dark'
          ? 'rgba(255,255,255,0.23)'
          : 'rgba(0,0,0,0.23)'
      }`,
    }}
    >
      {startEdorment && (
        <IconButton sx={{ p: '10px' }} aria-label="menu">
            {startEdorment}
        </IconButton>
      )}
      <InputBase
        name={placeholder}
        sx={{ ml: 1, flex: 1}}
        placeholder={placeholder || "Search Google Maps"}
        inputProps={{ 'aria-label': 'search google maps' }}
        value={value}
        onChange={(e) => {setInputValue(e.target.value); searchValue?.(e.target.value)}}
        onKeyDown={(e)=>{
            if(e.key === 'Enter') {
                handleSearch(e)
            }
        }}
      />
      {endEdorment && (
        <IconButton type="button" sx={{ p: '10px' }} aria-label="search" onClick={(e) => handleSearch(e)}>
            {endEdorment}
        </IconButton>
      )}
      {divider && <Divider sx={{ height: 28, m: 0.5 }} orientation="vertical" />}
      {optionalIcon && (
        <IconButton color="primary" sx={{ p: '10px' }} aria-label="directions">
            {optionalIcon}
        </IconButton>
       )}
    </Paper>
  );
}
