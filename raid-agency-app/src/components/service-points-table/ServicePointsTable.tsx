import { ServicePoint } from "@/generated/raid";
import { ServicePointWithMembers } from "@/types";
import {
  Cancel as CancelIcon,
  CheckCircleOutline as CheckCircleOutlineIcon,
  ErrorOutline as ErrorOutlineIcon,
} from "@mui/icons-material";
import { Button, Tooltip } from "@mui/material";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import { NavLink } from "react-router-dom";

const columns: GridColDef[] = [
  {
    field: "id",
    headerName: "ID",
    width: 125,
    renderCell: ({ value }) => {
      return (
        <NavLink to={`/service-points/${value}`}>
          <Button
            variant="outlined"
            size="small"
            fullWidth={true}
            sx={{ textTransform: "none" }}
          >
            {new Intl.NumberFormat("en-AU", {})
              .format(+value)
              .replace(/,/g, " ")}
          </Button>
        </NavLink>
      );
    },
  },
  {
    field: "enabled",
    headerName: "Enabled?",
    renderCell: ({ row }) => {
      return row.enabled ? (
        <CheckCircleOutlineIcon sx={{ color: "success.main" }} />
      ) : (
        <CancelIcon sx={{ color: "error.main" }} />
      );
    },
  },
  { field: "name", headerName: "Name", width: 350 },
  { field: "prefix", headerName: "Prefix", width: 175 },
  { field: "repositoryId", headerName: "Repository Id", width: 200 },
  {
    field: "groupId",
    headerName: "Group ID?",
    width: 120,
    renderCell: ({ row }) => <GroupIdCell row={row as ServicePointWithMembers} />,
  },
];

export function GroupIdCell({ row }: { row: ServicePointWithMembers }) {
  if (row.groupIdError) {
    return (
      <Tooltip title="Group ID is invalid" placement="left">
        <ErrorOutlineIcon sx={{ color: "error.main", pointerEvents: "auto", cursor: "help" }} />
      </Tooltip>
    );
  }
  return row.groupId ? (
    <Tooltip title={row.groupId} placement="left">
      <CheckCircleOutlineIcon sx={{ color: "success.main" }} />
    </Tooltip>
  ) : (
    <Tooltip title="No group ID configured" placement="left">
      <CancelIcon sx={{ color: "error.main", pointerEvents: "auto", cursor: "help" }} />
    </Tooltip>
  );
}

export function ServicePointsTable({
  servicePoints,
}: {
  servicePoints: (ServicePoint | ServicePointWithMembers)[];
}) {
  return (
    <DataGrid
      rows={servicePoints}
      columns={columns}
      rowSelection={false}
      density="compact"
      autoHeight
      isRowSelectable={() => false}
      getRowClassName={({ row }) =>
        (row as ServicePointWithMembers).groupIdError ? "row--group-id-error" : ""
      }
      initialState={{
        sorting: {
          sortModel: [{ field: "id", sort: "asc" }],
        },
        pagination: {
          paginationModel: {
            pageSize: 25,
          },
        },
      }}
      pageSizeOptions={[5, 10, 25, 50]}
      disableRowSelectionOnClick
      sx={{
        "& .row--group-id-error": {
          opacity: 0.5,
          pointerEvents: "none",
        },
        "& .MuiDataGrid-row.Mui-hovered": {
          backgroundColor: "transparent",
        },
        "& .MuiDataGrid-row:hover": {
          backgroundColor: "transparent",
        },
      }}
    />
  );
}
