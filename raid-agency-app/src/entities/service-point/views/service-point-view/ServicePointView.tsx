import {Card, CardContent, CardHeader, Grid} from "@mui/material";
import {useQuery} from "@tanstack/react-query";
import {fetchServicePoints} from "@/services/service-points";
import {useKeycloak} from "@/contexts/keycloak-context";
import {DisplayItem} from "@/components/display-item";
import {useMemo} from "react";

interface ServicePointViewProps {
    servicePointId: number;
}

export const ServicePointView = ({servicePointId}: ServicePointViewProps) => {
    const {authenticated, isInitialized, token} = useKeycloak();

    const servicePointsQuery = useQuery({
        queryKey: ["service-points"],
        queryFn: () => fetchServicePoints({token: token!}),
        enabled: isInitialized && authenticated && !!token,
    });

    // Move useMemo BEFORE any conditional returns
    const servicePoint = useMemo(() => {
        return servicePointsQuery.data?.find((servicePoint) => {
            return servicePoint.id === servicePointId;
        });
    }, [servicePointsQuery.data, servicePointId]);

    // Now you can have conditional returns
    if (servicePointsQuery.isLoading) {
        return <div>Loading service points...</div>;
    }

    if (servicePointsQuery.isError) {
        return <div>Error loading service points...</div>;
    }

    if (!servicePoint) {
        return <div>Unable to find service point ({servicePointId})...</div>;
    }

    return (
        <Card>
            <CardHeader title="Service Point"/>
            <CardContent>
                <Grid container spacing={2}>
                    <DisplayItem
                        label="Service Point"
                        value={servicePoint.name || ''}
                        width={6}
                    />
                </Grid>
            </CardContent>
        </Card>
    );
};