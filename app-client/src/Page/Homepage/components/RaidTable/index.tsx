import {FindAllRaidsRequest, RaidDto} from "../../../../Generated/Raidv2";
import React from "react";
import {useAuthApi} from "../../../../Api/AuthApi";
import {RqQuery} from "../../../../Util/ReactQueryUtil";
import {useQuery} from "@tanstack/react-query";
import {CompactErrorPanel} from "../../../../Error/CompactErrorPanel";
import {DataGrid, GridColDef, GridToolbar} from "@mui/x-data-grid";

import {
    Add as AddIcon,
    Edit as EditIcon,
    Menu as MenuIcon,
    OpenInNew as OpenInNewIcon,
    Visibility as VisibilityIcon
} from "@mui/icons-material"

import {
    Alert,
    Card,
    CardContent,
    CardHeader,
    Fab,
    IconButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem
} from "@mui/material";
import {handleColumn} from "./columns/handleColumn";
import {titleColumn} from "./columns/titleColumn";
import {startDateColumn} from "./columns/startDateColumn";
import {endDateColumn} from "./columns/endDateColumn";
import ContentCopy from '@mui/icons-material/ContentCopy';
import copy from "clipboard-copy";
import Divider from "@mui/material/Divider";
import {useAuth} from "../../../../Auth/AuthProvider";


export default function RaidTable() {
    const api = useAuthApi();
    const auth = useAuth();

    const servicePointId = auth.session.payload.servicePointId

    const [prefix, setPrefix] = React.useState<string>("")
    const [suffix, setSuffix] = React.useState<string>("")

    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
    const open = Boolean(anchorEl);
    const handleContextMenuClick = (event: React.MouseEvent<HTMLButtonElement>, rowData: RaidDto) => {
        const identifierSplit = rowData?.identifier?.id.split("/") || []
        const suffix = identifierSplit[identifierSplit.length - 1] || "";
        const prefix = identifierSplit[identifierSplit.length - 2] || "";

        setPrefix(prefix)
        setSuffix(suffix)
        setAnchorEl(event.currentTarget);
    };
    const handleClose = () => {
        setAnchorEl(null);
        setTimeout(() => {
            setPrefix("")
            setSuffix("")
        }, 500)

    };


    const listRaids = async ({servicePointId}: FindAllRaidsRequest) => {
        return await api.raid.findAllRaids({
            servicePointId,
        });
    };

    const raidQuery: RqQuery<RaidDto[]> = useQuery(
        ["listRaids", servicePointId],
        () => listRaids({servicePointId}),
    );

    const spQuery = useQuery(
        ["readServicePoint", servicePointId],
        async () =>
            await api.servicePoint.findServicePointById({
                id: servicePointId!,
            }),
    );

    const appWritesEnabled = spQuery.data?.appWritesEnabled;

    if (raidQuery.error) {
        return <CompactErrorPanel error={raidQuery.error}/>;
    }

    const columns: GridColDef[] = [
        handleColumn,
        titleColumn,
        startDateColumn,
        endDateColumn,
        {
            field: "_",
            headerName: "",
            disableColumnMenu: true,
            width: 25,
            disableExport: true,
            disableReorder: true,
            filterable: false,
            hideable: false,
            renderCell: (params) => {
                return (
                    <IconButton aria-label="more actions"
                                onClick={(event) => handleContextMenuClick(event, params.row)}>
                        <MenuIcon/>
                    </IconButton>
                );
            },
            sortable: false,
        }
    ];

    return (
        <>
            <Menu
                anchorEl={anchorEl}
                open={open}
                onClose={handleClose}
            >
                <MenuItem onClick={async () => {
                    await copy(`${suffix}`);
                    handleClose()
                }}>
                    <ListItemIcon>
                        <ContentCopy fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText primary="Copy Suffix" secondary={`${suffix}`}/>
                </MenuItem>
                <MenuItem onClick={async () => {
                    await copy(`${prefix}/${suffix}`);
                    handleClose()
                }}>
                    <ListItemIcon>
                        <ContentCopy fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText primary="Copy Handle" secondary={`${prefix}/${suffix}`}/>
                </MenuItem>
                <Divider/>
                <MenuItem onClick={() => {
                    handleClose()
                    window.location.href = `/show-raid/${prefix}/${suffix}`
                }}>

                    <ListItemIcon>
                        <VisibilityIcon fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText>Show RAiD</ListItemText>

                </MenuItem>
                <MenuItem onClick={() => {
                    handleClose()
                    window.location.href = `/show-raid/${prefix}/${suffix}`
                }}>

                    <ListItemIcon>
                        <EditIcon fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText>Edit RAiD</ListItemText>

                </MenuItem>
                <Divider/>
                <MenuItem onClick={() => {
                    handleClose()
                    window.location.href = `https://doi.test.datacite.org/dois/${prefix}%2F${suffix}`
                }}>
                    <ListItemIcon>
                        <OpenInNewIcon fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText primary="Open in Datacite" secondary="Must be signed in to Fabrica to display"/>

                </MenuItem>
            </Menu>
            {!appWritesEnabled && !spQuery.isLoading ? (
                <Alert severity="warning">
                    Editing is disabled for this service point.
                </Alert>
            ) : (
                <></>
            )}

            <Fab
                variant="extended"
                color="primary"
                sx={{position: "fixed", bottom: "16px", right: "16px"}}
                component="button"
                type="submit"
                href={"/mint-raid-new/20000000"}
            >
                <AddIcon sx={{mr: 1}}/>
                Mint new RAiD
            </Fab>

            <Card className="raid-card">
                <CardHeader title="Recently minted RAiD data" />
                <CardContent>
                    {raidQuery.data && (
                        <DataGrid
                            slots={{ toolbar: GridToolbar }}
                            slotProps={{ toolbar: { printOptions: { disableToolbarButton: true } } }}
                            rows={raidQuery.data}
                            columns={columns}
                            density="compact"
                            autoHeight
                            isRowSelectable={() => false}
                            getRowId={(row) => row.identifier.id}
                            initialState={{
                                pagination: {paginationModel: {pageSize: 10}},
                                columns: {
                                    columnVisibilityModel: {
                                        avatar: false,
                                        primaryDescription: false,
                                    },
                                },
                            }}
                            pageSizeOptions={[10, 25, 50, 100]}
                            sx={{// Neutralize the hover colour (causing a flash)
                                "& .MuiDataGrid-row.Mui-hovered": {
                                    backgroundColor: "transparent",
                                },
                                // Take out the hover colour
                                "& .MuiDataGrid-row:hover": {
                                    backgroundColor: "transparent",
                                },
                            }}
                        />
                    )}
                </CardContent>
            </Card>
        </>
    );
}