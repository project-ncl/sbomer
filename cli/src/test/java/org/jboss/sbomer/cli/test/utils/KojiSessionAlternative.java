package org.jboss.sbomer.cli.test.utils;

import java.util.List;
import java.util.Map;

import org.jboss.pnc.build.finder.koji.ClientSession;

import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveQuery;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveType;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiIdOrName;
import com.redhat.red.build.koji.model.xmlrpc.KojiRpmInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTaskInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTaskRequest;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

@Alternative
@Singleton
public class KojiSessionAlternative implements ClientSession {

    @Override
    public List<KojiArchiveInfo> listArchives(KojiArchiveQuery query) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'listArchives'");
    }

    @Override
    public Map<String, KojiArchiveType> getArchiveTypeMap() throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getArchiveTypeMap'");
    }

    @Override
    public KojiBuildInfo getBuild(int buildId) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getBuild'");
    }

    @Override
    public KojiTaskInfo getTaskInfo(int taskId, boolean request) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getTaskInfo'");
    }

    @Override
    public KojiTaskRequest getTaskRequest(int taskId) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getTaskRequest'");
    }

    @Override
    public List<KojiTagInfo> listTags(int id) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'listTags'");
    }

    @Override
    public void enrichArchiveTypeInfo(List<KojiArchiveInfo> archiveInfos) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'enrichArchiveTypeInfo'");
    }

    @Override
    public List<List<KojiArchiveInfo>> listArchives(List<KojiArchiveQuery> queries) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'listArchives'");
    }

    @Override
    public List<KojiBuildInfo> getBuild(List<KojiIdOrName> idsOrNames) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getBuild'");
    }

    @Override
    public List<KojiRpmInfo> getRPM(List<KojiIdOrName> idsOrNames) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getRPM'");
    }

    @Override
    public List<KojiTaskInfo> getTaskInfo(List<Integer> taskIds, List<Boolean> requests) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getTaskInfo'");
    }

    @Override
    public List<List<KojiRpmInfo>> listBuildRPMs(List<KojiIdOrName> idsOrNames) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'listBuildRPMs'");
    }

    @Override
    public List<List<KojiTagInfo>> listTags(List<KojiIdOrName> idsOrNames) throws KojiClientException {
        throw new UnsupportedOperationException("Unimplemented method 'listTags'");
    }

}
