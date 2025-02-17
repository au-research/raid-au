import { RaidDto } from "@/generated/raid";
import { Loading } from "@/pages/loading";
import { fetchRaids } from "@/services/raid";
import {
  Alert,
  Card,
  CardContent,
  CardHeader,
  LinearProgress,
  Stack,
  TableContainer,
  Typography,
} from "@mui/material";
import { DataGrid, GridColDef, GridToolbar } from "@mui/x-data-grid";

import { useQuery } from "@tanstack/react-query";
import { endDateColumn } from "./columns/endDateColumn";
import { handleColumn } from "./columns/handleColumn";
import { startDateColumn } from "./columns/startDateColumn";
import { titleColumn } from "./columns/titleColumn";
import { RaidTableRowContextMenu } from "./components";
import { useKeycloak } from "@/contexts/keycloak-context";

export const RaidTable = ({ title }: { title?: string }) => {
  const { authenticated, token, isInitialized } = useKeycloak();

  const raidQuery = useQuery<RaidDto[]>({
    queryKey: ["listRaids"],
    queryFn: () =>
      fetchRaids({
        fields: ["identifier", "title", "date"],
        token: token!,
      }),
    enabled: isInitialized && authenticated,
  });

  const appWritesEnabled = true;

  if (raidQuery.isPending) {
    return <Loading />;
  }

  if (raidQuery.isError) {
    return <Typography variant="h6">No data.</Typography>;
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
      renderCell: (params) => <RaidTableRowContextMenu row={params.row} />,
      sortable: false,
    },
  ];

  return (
    <>
      {!appWritesEnabled ? (
        <Alert severity="warning">
          Editing is disabled for this service point.
        </Alert>
      ) : (
        <></>
      )}

      <Card>
        {title && <CardHeader title={title} />}
        <CardContent>
          <input type="hidden" value={raidQuery.data.length} id="raid-count" />
          <TableContainer>
            {raidQuery.isPending && (
              <>
                <Stack gap={2}>
                  <Typography>Loading RAiDs...</Typography>
                  <LinearProgress />
                </Stack>
              </>
            )}
            {raidQuery.data && (
              <DataGrid
                loading={raidQuery.isPending}
                slots={{ toolbar: GridToolbar }}
                slotProps={{
                  toolbar: { printOptions: { disableToolbarButton: true } },
                }}
                rows={raidQuery.data}
                columns={columns}
                density="compact"
                autoHeight
                isRowSelectable={() => false}
                getRowId={(row) => row.identifier.id}
                initialState={{
                  pagination: {
                    paginationModel: { pageSize: 10 },
                  },
                  columns: {
                    columnVisibilityModel: {
                      avatar: false,
                      primaryDescription: false,
                    },
                  },
                }}
                pageSizeOptions={[10, 25, 50, 100]}
                sx={{
                  // Neutralize the hover colour (causing a flash)
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
          </TableContainer>
        </CardContent>
      </Card>
    </>
  );
};
