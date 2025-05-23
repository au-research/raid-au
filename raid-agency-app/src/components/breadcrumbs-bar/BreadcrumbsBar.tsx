import type { Breadcrumb } from "../../generated/raid/Breadcrumb";
import { NavigateNext as NavigateNextIcon } from "@mui/icons-material";
import { Breadcrumbs, Button, Card, Paper } from "@mui/material";
import { memo } from "react";
import { Link } from "react-router-dom";

export const BreadcrumbsBar = memo(
  ({ breadcrumbs }: { breadcrumbs: Breadcrumb[] }) => (
    <Card>
      <Breadcrumbs
        component={Paper}
        aria-label="breadcrumb"
        separator={<NavigateNextIcon fontSize="small" />}
        variant="outlined"
        sx={{ p: 1 }}
      >
        {breadcrumbs.map((el, i: number) => (
          <Button
            key={el.to}
            component={Link}
            variant="outlined"
            size="small"
            fullWidth={true}
            sx={{ textTransform: "none" }}
            startIcon={el.icon}
            to={el.to}
            disabled={i === breadcrumbs.length - 1}
          >
            {el.label}
          </Button>
        ))}
      </Breadcrumbs>
    </Card>
  )
);

BreadcrumbsBar.displayName = "BreadcrumbsBar";
