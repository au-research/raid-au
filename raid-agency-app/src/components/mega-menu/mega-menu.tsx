import { Button, Paper, Stack, Typography } from "@mui/material";
import React from "react";
import { ChevronDown, ChevronUp } from "lucide-react";

export const MegaMenu = () => {
    const [toggleBtn, setToggleBtn] = React.useState(false);
    return (
        <Paper elevation={1} sx={{width: '100%', zIndex: 1200, position: 'relative'}}>
            <Stack direction="row" spacing={2} justifyContent="right" alignItems="center">
                <Button onClick={() => setToggleBtn(!toggleBtn)}>
                    <Typography sx={{textTransform: "capitalize", color: "text.primary", display: "contents", alignItems: "center"}}>{("Explore").toLowerCase()} ARDC {toggleBtn ? <ChevronUp  strokeWidth={3} absoluteStrokeWidth style={{marginLeft: "4px", fontWeight: "bold"}} size={20} /> : <ChevronDown  strokeWidth={3} absoluteStrokeWidth style={{marginLeft: "4px"}} size={20} />}</Typography>
                </Button>
            </Stack>
        </Paper>
    );
}
        