import type { Breadcrumb } from "@/components/breadcrumbs-bar";
import { BreadcrumbsBar } from "@/components/breadcrumbs-bar";
import Footer from "@/components/footer-bar/footer";
import { RaidTable } from "@/pages/raid-table";
import {
  HistoryEdu as HistoryEduIcon,
  Home as HomeIcon,
} from "@mui/icons-material";
import { Container, Stack } from "@mui/material";

export const Raids = () => {
  const breadcrumbs: Breadcrumb[] = [
    {
      label: "Home",
      to: "/",
      icon: <HomeIcon />,
    },
    {
      label: "RAiDs",
      to: "/raids",
      icon: <HistoryEduIcon />,
    },
  ];

  return (
    <>
      <Container maxWidth="lg" sx={{ minHeight: '100vh' }}>
        <Stack gap={2}>
          <BreadcrumbsBar breadcrumbs={breadcrumbs} />
          <RaidTable />
      </Stack>
      </Container>
      <Footer />
    </>
  );
};
