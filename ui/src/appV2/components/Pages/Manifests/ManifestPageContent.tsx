import { DefaultSbomerApiV2 } from '@appV2/api/DefaultSbomerApiV2';
import { useManifest } from '@appV2/components/Pages/Manifests/useManifest';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import { ErrorSection } from '@appV2/components/Sections/ErrorSection/ErrorSection';
import {
  Grid,
  Column,
  Button,
  SkeletonText,
  CodeSnippet,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  Content,
  Heading,
  ButtonSet,
  Stack,
} from '@carbon/react';
import { Download } from '@carbon/icons-react';
import * as React from 'react';
import { useParams } from 'react-router-dom';

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
    return <ErrorSection error={error} />;
  }

  if (loading) {
    return <SkeletonText />;
  }

  if (!manifest) {
    return null;
  }

  return (
    <Content>
      <Stack gap={7}>
        <Grid>
          <Column sm={4} md={8} lg={16}>
            <Heading>Manifest {id}</Heading>
          </Column>
        </Grid>

        <Grid>
          <Column sm={4} md={8} lg={16}>
            <StructuredListWrapper>
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
                    <CodeSnippet type="inline" hideCopyButton>
                      {manifest.id}
                    </CodeSnippet>
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>Created</StructuredListCell>
                  <StructuredListCell>
                    {manifest.created ? manifest.created.toISOString() : 'N/A'}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListBody>
            </StructuredListWrapper>
          </Column>
        </Grid>

        <Grid>
          <Column sm={4} md={8} lg={16}>
            <ButtonSet>
              <Button
                kind="primary"
                renderIcon={Download}
                onClick={(e) => downloadManifest(manifest)}
              >
                Download
              </Button>
            </ButtonSet>
          </Column>
        </Grid>

        <Grid>
          <Column sm={4} md={8} lg={16}>
            <Stack gap={5}>
              <Heading>Attributes</Heading>
              <CodeSnippet type="multi">
                {JSON.stringify(manifest, null, 2)}
              </CodeSnippet>
            </Stack>
          </Column>
        </Grid>
      </Stack>
    </Content>
  );
};

export { ManifestPageContent };
