package org.jenkinsci.plugins.gitlab;

import static org.jenkinsci.plugins.gitlab.GitlabConstants.*;
import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.plugins.git.GitException;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserMergeOptions;
import hudson.plugins.git.extensions.GitSCMExtensionDescriptor;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.PreBuildMerge;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RemoteConfig;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.MergeCommand;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * pre build merge for gitlab merge request.
 * 
 * @author Hiroyuki Wada
 * 
 */
public class GitlabPreBuildMerge extends GitSCMExtension {

    private String mergeRemote;
    private String mergeStrategy;

    @DataBoundConstructor
    public GitlabPreBuildMerge(String mergeRemote, String mergeStrategy) {
        this.mergeRemote = mergeRemote;
        this.mergeStrategy = mergeStrategy;
    }

    public String getMergeRemote() {
        return mergeRemote;
    }

    public String getMergeStrategy() {
        return mergeStrategy;
    }

    @Override
    public void beforeCheckout(GitSCM scm, AbstractBuild<?, ?> build,
            GitClient git, BuildListener listener) throws IOException,
            InterruptedException, GitException {

        String gitlabSourceBranch = build.getEnvironment(listener).expand(
                $SOURCE_BRANCH_KEY$);
        String gitlabSourceUrl = build.getEnvironment(listener).expand(
                $SOURCE_URL_KEY$);

        if (gitlabSourceBranch.equals($SOURCE_BRANCH_KEY$)
                || gitlabSourceUrl.equals($SOURCE_URL_KEY$)) {
            throw new GitException("Error trying to resolve "
                    + $SOURCE_BRANCH_KEY$ + " and " + $SOURCE_URL_KEY$
                    + ". Resolved values are [" + gitlabSourceBranch + "], "
                    + gitlabSourceUrl + "]");
        }

        // replace PreBuildMerge extension at runtime
        UserMergeOptions userMergeOptions = new UserMergeOptions(mergeRemote,
                $TARGET_BRANCH_KEY$, mergeStrategy);
        scm.getExtensions().replace(new PreBuildMerge(userMergeOptions));

        // setup BranchSpec
        List<BranchSpec> branches = scm.getBranches();
        branches.clear();
        branches.add(new BranchSpec(SOURCE_REMOTE_BRANCH_NAME + "/"
                + gitlabSourceBranch));

        // add remote repository of gitlabSourceBranch
        removeRemoteRepository(scm.getRepositories(), SOURCE_REMOTE_BRANCH_NAME);
        scm.getRepositories().add(
                newRemoteConfig(SOURCE_REMOTE_BRANCH_NAME, gitlabSourceUrl));
    }

    @Override
    public void onCheckoutCompleted(GitSCM scm, AbstractBuild<?, ?> build,
            GitClient git, BuildListener listener) throws IOException,
            InterruptedException, GitException {
        // restore extensions
        scm.getExtensions().remove(PreBuildMerge.class);

        // restore BranchSpec
        scm.getBranches().clear();
        scm.getBranches().add(new BranchSpec("**"));

        // restore remote repository
        removeRemoteRepository(scm.getRepositories(), SOURCE_REMOTE_BRANCH_NAME);
    }

    private void removeRemoteRepository(List<RemoteConfig> repositories,
            String removeRepositoryName) {
        List<RemoteConfig> newRepositories = new ArrayList<RemoteConfig>();
        for (RemoteConfig remoteConfig : repositories) {
            if (!remoteConfig.getName().equals(removeRepositoryName)) {
                newRepositories.add(remoteConfig);
            }
        }
        repositories.clear();
        repositories.addAll(newRepositories);
    }

    @Extension
    public static class DescriptorImpl extends GitSCMExtensionDescriptor {
        @Override
        public String getDisplayName() {
            return "Setup Pre Build Merge for Gitlab Merge Request";
        }

        public ListBoxModel doFillMergeStrategyItems() {
            ListBoxModel m = new ListBoxModel();
            for (MergeCommand.Strategy strategy : MergeCommand.Strategy
                    .values())
                m.add(strategy.toString(), strategy.toString());
            return m;
        }
    }

    private RemoteConfig newRemoteConfig(String name, String refUrl) {
        try {
            Config repoConfig = new Config();
            repoConfig.setString("remote", name, "url", refUrl);
            repoConfig.setString("remote", name, "fetch",
                    "+refs/heads/*:refs/remotes/" + name + "/*");
            return new RemoteConfig(repoConfig, name);
        } catch (URISyntaxException ex) {
            throw new GitException(
                    "Error trying to add remote repository for [" + refUrl
                            + "]", ex);
        }
    }
}
