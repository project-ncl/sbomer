import { DefaultSbomerApiV2 } from '@appV2/api/DefaultSbomerApiV2';
import { useManifest } from '@appV2/components/Pages/Manifests/useManifest';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import { MetadataOverview } from '@appV2/components/UtilsComponents/MetadataOverview';
import RelativeTimestamp from '@appV2/components/UtilsComponents/RelativeTimestamp';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { Download } from '@carbon/icons-react';
import {
  Button,
  ButtonSet,
  CodeSnippet,
  Heading,
  SkeletonText,
  Stack,
  StructuredListBody,
  StructuredListCell,
  StructuredListHead,
  StructuredListRow,
  StructuredListWrapper,
} from '@carbon/react';
import * as React from 'react';
import { Meta, useParams } from 'react-router-dom';

const ManifestPageContent: React.FunctionComponent = () => {
  const { id } = useParams<{ id: string }>();
  const [{ request: manifest, error, loading }] = useManifest(id!);

  useDocumentTitle('SBOMer | Manifests | ' + id);

  const downloadManifest = async (manifest: any) => {
    try {
      const json = await DefaultSbomerApiV2.getInstance().getManifestJson(manifest.id);
      const blob = new Blob([JSON.stringify(json)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const element = document.createElement('a');
      element.href = url;
      element.download = `${manifest.id}.json`;
      document.body.appendChild(element);
      element.click();
      document.body.removeChild(element);
      URL.revokeObjectURL(url);
    } catch (e: any) {
      alert('Download failed: ' + e.message);
    }
  };

  if (error) {
    return <ErrorSection title="Could not load manifests" message={error.message} />;
  }

  if (loading) {
    return <SkeletonText />;
  }

  if (!manifest) {
    return null;
  }

  return (
    <Stack gap={7}>
      <Heading>Manifest {id}</Heading>

        <StructuredListWrapper isCondensed>
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>Property</StructuredListCell>
              <StructuredListCell head>Value</StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            <StructuredListRow>
              <StructuredListCell>ID</StructuredListCell>
              <StructuredListCell>
                <span >
                  {manifest.id}
                </span>
              </StructuredListCell>
            </StructuredListRow>
            <StructuredListRow>
              <StructuredListCell>Created</StructuredListCell>
              <StructuredListCell>
                {manifest.created ? (
                  <Stack gap={2}>
                    <RelativeTimestamp date={manifest.created} />
                    <span>{manifest.created.toISOString()}</span>
                  </Stack>
                ) : 'N/A'}
              </StructuredListCell>
            </StructuredListRow>
          </StructuredListBody>
        </StructuredListWrapper>

        <ButtonSet>
          <Button
            kind="primary"
            renderIcon={Download}
            onClick={(e) => downloadManifest(manifest)}
          >
            Download
          </Button>
        </ButtonSet>
        <MetadataOverview metadata={manifest.metadata} redirectPrefix="manifests" />

        <Stack gap={5}>
          <Heading>Raw JSON</Heading>
          <CodeSnippet type="multi">
            {JSON.stringify(manifest, null, 2)}
          </CodeSnippet>
        </Stack>
      </Stack>
  );
};

export { ManifestPageContent };
