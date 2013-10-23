package org.jenkinsci.plugins.gitlab;

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
 * 
 * @author Hiroyuki Wada
 * 
 */
public class GitlabAutoConfig extends GitSCMExtension {

    private PreBuildMerge preBuildMerge;

    @DataBoundConstructor
    public GitlabAutoConfig(String mergeRemote, String mergeStrategy) {
        UserMergeOptions userMergeOptions = new UserMergeOptions(mergeRemote,
                "${gitlabTargetBranch}", mergeStrategy);
        preBuildMerge = new PreBuildMerge(userMergeOptions);
    }

    @Override
    public void beforeCheckout(GitSCM scm, AbstractBuild<?, ?> build,
            GitClient git, BuildListener listener) throws IOException,
            InterruptedException, GitException {

        String gitlabSourceBranch = build.getEnvironment(listener).expand(
                "${gitlabSourceBranch}");
        String gitlabSourceUrl = build.getEnvironment(listener).expand(
                "${gitlabSourceUrl}");

        if (gitlabSourceBranch.equals("${gitlabSourceBranch}")
                || gitlabSourceUrl.equals("${gitlabSourceUrl}")) {
            throw new GitException(
                    "Error trying to resolve ${gitlabSourceBranch} and ${gitlabSourceUrl}. Resolved values are ["
                            + gitlabSourceBranch
                            + "], "
                            + gitlabSourceUrl
                            + "]");
        }

        // add PreBuildMerge extension
        scm.getExtensions().replace(preBuildMerge);

        // setup BranchSpec
        List<BranchSpec> branches = scm.getBranches();
        branches.clear();
        branches.add(new BranchSpec("gitlabSource/" + gitlabSourceBranch));

        // add remote repository of gitlabSourceBranch
        List<RemoteConfig> newRepositories = new ArrayList<RemoteConfig>();
        List<RemoteConfig> repositories = scm.getRepositories();
        for (RemoteConfig remoteConfig : repositories) {
            if (!remoteConfig.getName().equals("gitlabSource")) {
                newRepositories.add(remoteConfig);
            }
        }
        newRepositories.add(newRemoteConfig("gitlabSource", gitlabSourceUrl));
        repositories.clear();
        repositories.addAll(newRepositories);
    }

    @Extension
    public static class DescriptorImpl extends GitSCMExtensionDescriptor {
        @Override
        public String getDisplayName() {
            return "Setup Gitlab Merge Request";
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
                    "+refs/heads/*:refs/remotes/gitlabSource/*");
            return new RemoteConfig(repoConfig, "gitlabSource");
        } catch (URISyntaxException ex) {
            throw new GitException("Error trying to create JGit configuration",
                    ex);
        }
    }

    private static class GitSCMWrapper extends GitSCM {
        private GitSCM scm;

        GitSCMWrapper(GitSCM scm) {
            super(null);
            this.scm = scm;
        }
    }
}
