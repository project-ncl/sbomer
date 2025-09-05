import React from "react";
import { AppLayout } from "@appV2/components/Pages/AppLayout/AppLayout";
import { useDocumentTitle } from "@appV2/utils/useDocumentTitle";
import { HelpPageContent } from "./HelpPageContent";



const HelpPage: React.FunctionComponent = () => {
  useDocumentTitle('SBOMer | Help');

  return (
    <AppLayout>
      <HelpPageContent />
    </AppLayout>
  );
};

export { HelpPage };
