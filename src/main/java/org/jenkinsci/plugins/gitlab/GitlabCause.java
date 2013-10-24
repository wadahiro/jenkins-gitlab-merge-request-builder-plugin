package org.jenkinsci.plugins.gitlab;

import hudson.model.Cause;

public class GitlabCause extends Cause {
    private final Integer _mergeRequestId;
    private final Integer _mergeRequestIid;
    private final String _sourceBranch;
    private final String _targetBranch;
    private final String _sourceUrl;

    public GitlabCause(Integer mergeRequestId, Integer mergeRequestIid,
            String sourceBranch, String targetBranch,
            String sourceUrl) {
        _mergeRequestId = mergeRequestId;
        _mergeRequestIid = mergeRequestIid;
        _sourceBranch = sourceBranch;
        _targetBranch = targetBranch;
        _sourceUrl = sourceUrl;
    }

    @Override
    public String getShortDescription() {
        return "Gitlab Merge Request #" + _mergeRequestIid + " : " + _sourceBranch + " => " + _targetBranch;
    }

    public Integer getMergeRequestId() {
        return _mergeRequestId;
    }

    public String getSourceBranch() {
        return _sourceBranch;
    }

    public String getTargetBranch() {
        return _targetBranch;
    }

    public Integer getMergeRequestIid() {
        return _mergeRequestIid;
    }

    public String getSourceUrl() {
        return _sourceUrl;
    }
}
