import {GridColDef} from "@mui/x-data-grid";
import {dateDisplayFormatter} from "../../../../../date-utils";

export const startDateColumn: GridColDef = {
    field: "date.startDate",
    headerName: "Start Date",
    width: 120,
    valueGetter: (params) => {
        return dateDisplayFormatter(params.row.date.startDate)
    }
}