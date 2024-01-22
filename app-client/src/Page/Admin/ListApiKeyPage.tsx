import {DateTimeDisplay, getRoleForKey} from "Component/Util";
import {TextSpan} from "Component/TextSpan";
import React from "react";
import {useQuery} from "@tanstack/react-query";
import {useAuthApi} from "Api/AuthApi";
import {CompactErrorPanel} from "Error/CompactErrorPanel";
import {
    Card,
    CardContent,
    CardHeader,
    Container,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow
} from "@mui/material";
import {RefreshIconButton} from "Component/RefreshIconButton";
import {RaidoLink} from "Component/RaidoLink";
import {Visibility, VisibilityOff} from "@mui/icons-material";
import {getViewApiKeyPageLink} from "Page/Admin/ApiKeyPage";
import {RaidoAddFab} from "Component/AppButton";
import {useParams} from "react-router-dom";


export function ListApiKeyPage() {
    const {servicePointId} = useParams() as { servicePointId: string }
    const api = useAuthApi();
    const apiKeysQuery = useQuery(['listApiKey', servicePointId],
        async () => await api.admin.listApiKey({servicePointId: parseInt(servicePointId)}));
    const servicePointQuery = useQuery(['readServicePoint', servicePointId],
        async () => await api.servicePoint.findServicePointById({id: parseInt(servicePointId)}));

    if (apiKeysQuery.error) {
        return <CompactErrorPanel error={apiKeysQuery.error}/>
    }

    if (apiKeysQuery.isLoading) {
        return <TextSpan>loading...</TextSpan>
    }

    if (!apiKeysQuery.data) {
        console.log("unexpected state", apiKeysQuery);
        return <TextSpan>unexpected state</TextSpan>
    }
    return (
        <Container>
            <Card>
                <CardHeader
                    title={`${servicePointQuery.data?.name ?? '...'} - API keys`}
                    action={<>
                        <RefreshIconButton
                            refreshing={apiKeysQuery.isLoading}
                            onClick={() => apiKeysQuery.refetch()}/>
                        <RaidoAddFab
                            disabled={false}
                            href={`/create-api-key/${(servicePointId)}?servicePointId=${servicePointId}`}/>
                    </>}/>
                <CardContent>
                    <TableContainer>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell>Subject</TableCell>
                                    <TableCell>Role</TableCell>
                                    <TableCell>Expires</TableCell>
                                    <TableCell align="center">Enabled</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {apiKeysQuery.data.map((row) => (
                                    <TableRow
                                        key={row.id}
                                        // don't render a border under last row
                                        sx={{'&:last-child td, &:last-child th': {border: 0}}}
                                    >
                                        <TableCell>
                                            <RaidoLink
                                                href={`${getViewApiKeyPageLink(row.id)}?servicePointId=${servicePointId}`}>
                                                {row.subject}
                                            </RaidoLink>
                                        </TableCell>
                                        <TableCell>
                                            {getRoleForKey(row.role)}
                                        </TableCell>
                                        <TableCell>
                                            <DateTimeDisplay date={row.tokenCutoff}/>
                                        </TableCell>
                                        <TableCell align="center">
                                            {row.enabled ?
                                                <Visibility color={"success"}/> :
                                                <VisibilityOff color={"error"}/>
                                            }
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>

                    </TableContainer>
                </CardContent>
            </Card>
        </Container>
    )
}