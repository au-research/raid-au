import {Card, CardContent, CardHeader, Stack} from "@mui/material";
import {useQuery} from "@tanstack/react-query";
import {fetchServicePoints} from "@/services/service-points";
import {useKeycloak} from "@/contexts/keycloak-context";
import {FieldErrors} from "react-hook-form";
import {RaidDto} from "@/generated/raid";
import {TextSelectField} from "@/components/fields/TextSelectField.tsx";
import { MetadataContext } from "@/components/raid-form/RaidForm";
import { CustomStyledTooltip } from "@/components/tooltips/StyledTooltip";
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import {useContext} from "react";

export const ServicePointForm = ({errors}: { errors: FieldErrors<RaidDto>}) => {
    const { authenticated, isInitialized, token } = useKeycloak();
    const labelPlural = "Service Points";
    const label = "Service Point";
    const keys = "servicePoint";
    const metadata = useContext(MetadataContext);
    const tooltip = metadata?.[keys]?.tooltip;
    const servicePointsQuery = useQuery({
        queryKey: ["service-points"],
        queryFn: () =>
            fetchServicePoints({
                token: token!,
            }),
        enabled: isInitialized && authenticated && !!token,
    });

    const options = servicePointsQuery.data?.map((servicePoint) => {
        return {
            label: servicePoint.name,
            value: servicePoint.id.toString(),
        };
    })

    const key = 'identifier.owner.servicePoint' as keyof FieldErrors<RaidDto>;

    if (servicePointsQuery.isLoading) {
        return <div>Loading service points...</div>;
    }

    if (servicePointsQuery.isError) {
        return <div>Error loading service points</div>;
    }
    return (
        <Card
            sx={{
                borderLeft: errors[key] ? "3px solid" : "none",
                borderLeftColor: "error.main",
            }}
            id={key}
        >
            <Stack direction="row" alignItems="center">
                <CardHeader sx={{padding: "16px 0 16px 16px"}} title={labelPlural} />
                <CustomStyledTooltip
                title={label}
                content={tooltip || ""}
                variant="info"
                placement="top"
                tooltipIcon={<InfoOutlinedIcon />}
                >
                </CustomStyledTooltip>
            </Stack>
            <CardContent>
                <TextSelectField
                    name="identifier.owner.servicePoint"
                    label="Service Point"
                    required={true}
                    width={3}
                    options={options || []}
                />
            </CardContent>
        </Card>
    );
};