import { DefaultSbomerApiV2 } from '@appV2/api/DefaultSbomerApiV2';
import { useManifest } from '@appV2/components/Pages/Manifests/useManifest';
import { useDocumentTitle } from '@appV2/utils/useDocumentTitle';
import {
  ActionList,
  ActionListItem,
  Alert,
  Button,
  CodeBlock,
  CodeBlockCode,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Grid,
  GridItem,
  PageSection,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  Title,
} from '@patternfly/react-core';
import { DownloadIcon } from '@patternfly/react-icons';
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
    return (
      <Alert isExpandable variant="warning" title="Cannot retrieve manifest">
        <p>{error.message}.</p>
      </Alert>
    );
  }

  if (loading) {
    return <Skeleton screenreaderText="Loading..." />;
  }

  if (!manifest) {
    return null;
  }

  return (
    <PageSection hasBodyWrapper={false}>
      <Grid hasGutter span={12}>
        <GridItem span={12}>
          <Title headingLevel="h1" size="4xl">
            Manifest {id}
          </Title>
        </GridItem>
        <GridItem span={12}>
          <DescriptionList
            columnModifier={{
              default: '2Col',
            }}
          >
            <DescriptionListGroup>
              <DescriptionListTerm>ID</DescriptionListTerm>
              <DescriptionListDescription>
                <pre>{manifest.id}</pre>
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Created</DescriptionListTerm>
              <DescriptionListDescription>
                <Timestamp date={manifest.creationTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                  {/* {timestampToHumanReadable(Date.now() - request.creationTime.getTime(), false, 'ago')} */}
                </Timestamp>
              </DescriptionListDescription>
            </DescriptionListGroup>

          </DescriptionList>

        </GridItem>

        <ActionList>
          <ActionListItem>
            <Button variant="primary" icon={<DownloadIcon />} onClick={(e) => downloadManifest(manifest)}>
              {' '}
              Download
            </Button>
          </ActionListItem>
        </ActionList>
        <GridItem span={12}>
          <DescriptionList>
            <DescriptionListGroup>
              <DescriptionListTerm>Attributes</DescriptionListTerm>
              <DescriptionListDescription>
                <CodeBlock >
                  <CodeBlockCode>
                    {JSON.stringify(manifest, null, 2)}
                  </CodeBlockCode>
                </CodeBlock>
              </DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>
        </GridItem>
      </Grid>
    </PageSection>
  );
};

export { ManifestPageContent };
