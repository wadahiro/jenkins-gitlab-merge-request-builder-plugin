package org.jenkinsci.plugins.gitlab;

public class GitlabConstants {
    public static final String SOURCE_REMOTE_BRANCH_NAME = "gitlabSource";

    public static final String MERGE_REQUEST_ID_KEY = "gitlabMergeRequestId";
    public static final String TARGET_BRANCH_KEY = "gitlabTargetBranch";
    public static final String SOURCE_BRANCH_KEY = "gitlabSourceBranch";
    public static final String SOURCE_URL_KEY = "gitlabSourceUrl";

    public static final String $TARGET_BRANCH_KEY$ = wrap(TARGET_BRANCH_KEY);
    public static final String $SOURCE_BRANCH_KEY$ = wrap(SOURCE_BRANCH_KEY);
    public static final String $SOURCE_URL_KEY$ = wrap(SOURCE_URL_KEY);

    private static String wrap(String key) {
        return "${" + key + "}";
    }
}
