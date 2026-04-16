import type { Breadcrumb } from "@/components/breadcrumbs-bar";
import { BreadcrumbsBar } from "@/components/breadcrumbs-bar";
import Footer from "@/components/footer-bar/footer";
import { RaidTable } from "@/pages/raid-table";
import {
  HistoryEdu as HistoryEduIcon,
  Home as HomeIcon,
} from "@mui/icons-material";
import { Container, Stack, Box } from "@mui/material";

export const Raids = () => {
  const isProduction = import.meta.env.VITE_RAIDO_ENV === 'prod';
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
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: 'calc(100vh - ' + (isProduction ? '168px' : '216px') + ')' }}> 
      <Container maxWidth="lg" sx={{ flex: 1, mb: 5 }}>
        <Stack gap={2}>
          <BreadcrumbsBar breadcrumbs={breadcrumbs} />
          <RaidTable />
      </Stack>
      </Container>
      <Footer />
    </Box>
  );
};
