/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.pullrequest.client;

import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.api.constraints.Constraints.FIRST;
import static org.eclipse.che.ide.api.parts.PartStackType.TOOLING;
import static org.eclipse.che.plugin.pullrequest.shared.ContributionProjectTypeConstants.CONTRIBUTE_TO_BRANCH_VARIABLE_NAME;
import static org.eclipse.che.plugin.pullrequest.shared.ContributionProjectTypeConstants.CONTRIBUTION_PROJECT_TYPE_ID;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.factory.FactoryAcceptedEvent;
import org.eclipse.che.ide.api.parts.PartStack;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.Selection.NoSelectionProvided;
import org.eclipse.che.ide.api.selection.SelectionChangedEvent;
import org.eclipse.che.ide.api.workspace.WorkspaceReadyEvent;
import org.eclipse.che.ide.api.workspace.event.WorkspaceStoppedEvent;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.plugin.pullrequest.client.parts.contribute.ContributePartPresenter;
import org.eclipse.che.plugin.pullrequest.client.vcs.VcsService;
import org.eclipse.che.plugin.pullrequest.client.vcs.VcsServiceProvider;
import org.eclipse.che.plugin.pullrequest.client.vcs.hosting.VcsHostingServiceProvider;
import org.eclipse.che.plugin.pullrequest.client.workflow.Context;
import org.eclipse.che.plugin.pullrequest.client.workflow.WorkflowExecutor;

/**
 * Responsible for setting up contribution mixin for the currently selected project in application
 * context.
 *
 * @author Vlad Zhukovskyi
 * @since 5.0.0
 */
@Singleton
public class ContributionMixinProvider {

  private final EventBus eventBus;
  private final AppContext appContext;
  private final WorkspaceAgent workspaceAgent;
  private final ContributePartPresenter contributePart;
  private final WorkflowExecutor workflowExecutor;
  private final VcsServiceProvider vcsServiceProvider;
  private final VcsHostingServiceProvider vcsHostingServiceProvider;
  private final PromiseProvider promiseProvider;
  private final ContributeMessages messages;

  private HandlerRegistration handlerRegistration;

  private Project lastSelected;

  @Inject
  public ContributionMixinProvider(
      EventBus eventBus,
      AppContext appContext,
      WorkspaceAgent workspaceAgent,
      ContributePartPresenter contributePart,
      WorkflowExecutor workflowExecutor,
      VcsServiceProvider vcsServiceProvider,
      VcsHostingServiceProvider vcsHostingServiceProvider,
      PromiseProvider promiseProvider,
      ContributeMessages messages) {
    this.eventBus = eventBus;
    this.appContext = appContext;
    this.workspaceAgent = workspaceAgent;
    this.contributePart = contributePart;
    this.workflowExecutor = workflowExecutor;
    this.vcsServiceProvider = vcsServiceProvider;
    this.vcsHostingServiceProvider = vcsHostingServiceProvider;
    this.promiseProvider = promiseProvider;
    this.messages = messages;

    if (appContext.getFactory() != null) {
      handlerRegistration =
          eventBus.addHandler(
              FactoryAcceptedEvent.TYPE,
              event -> {
                addPart();
                handlerRegistration.removeHandler();

                subscribeToSelectionChangedEvent();
              });
    } else {
      subscribeToSelectionChangedEvent();
      eventBus.addHandler(WorkspaceReadyEvent.getType(), event -> addPart());
    }

    eventBus.addHandler(
        WorkspaceStoppedEvent.TYPE,
        event -> {
          workspaceAgent.removePart(contributePart);
        });
  }

  private void subscribeToSelectionChangedEvent() {
    eventBus.addHandler(
        SelectionChangedEvent.TYPE, event -> processCurrentProject(event.getSelection()));
  }

  private void processCurrentProject(Selection<?> selection) {
    if (selection instanceof NoSelectionProvided) {
      return;
    }

    if (selection.isMultiSelection()) {
      contributePart.showStub(messages.stubTextContributionPanelNotSupportMultiSelection());
      lastSelected = null;
      return;
    }

    final Project rootProject = appContext.getRootProject();

    if (lastSelected != null && lastSelected.equals(rootProject)) {
      return;
    }

    contributePart.showStub(messages.stubTextLoading());

    if (rootProject == null) {
      invalidateContext(lastSelected);
      contributePart.showStub(messages.stubTextProjectIsNotSelected());
    } else if (hasVcsService(rootProject)) {
      handleProjectWithVCS(rootProject);
    } else {
      invalidateContext(rootProject);
      contributePart.showStub(messages.stubTextProjectNotProvideSupportedVSC());
    }
    lastSelected = rootProject;
  }

  private void handleProjectWithVCS(Project prj) {
    if (hasContributionMixin(prj)) {
      vcsHostingServiceProvider
          .getVcsHostingService(prj)
          .then(
              vcsHostingService -> {
                workflowExecutor.init(vcsHostingService, prj);
                contributePart.showContent();
              })
          .catchError(
              err -> {
                handleVCSError(
                    prj, messages.failedToGetVSCService(prj.getName(), err.getMessage()));
              });
    } else {
      vcsHostingServiceProvider
          .getVcsHostingService(prj)
          .then(
              vcsHostingService -> {
                addMixin(prj)
                    .then(
                        prjWithMixin -> {
                          workflowExecutor.init(vcsHostingService, prjWithMixin);
                          contributePart.showContent();

                          lastSelected = prjWithMixin;
                        })
                    .catchError(
                        err -> {
                          handleVCSError(
                              prj, messages.failedToApplyVSCMixin(prj.getName(), err.getMessage()));
                        });
              })
          .catchError(
              err -> {
                handleVCSError(
                    prj, messages.failedToGetVSCService(prj.getName(), err.getMessage()));
              });
    }
  }

  private void handleVCSError(Project project, String logError) {
    Log.error(getClass(), logError);
    invalidateContext(project);
    contributePart.showStub(messages.stubTextNothingToShow());
  }

  private void invalidateContext(Project project) {
    if (project != null) {
      final Optional<Context> context = workflowExecutor.getContext(project.getName());
      if (context.isPresent()) {
        workflowExecutor.invalidateContext(context.get().getProject());
      }
    }
  }

  private void addPart() {
    PartStack partStack = workspaceAgent.getPartStack(TOOLING);
    if (!partStack.containsPart(contributePart)) {
      partStack.addPart(contributePart, FIRST);
      if (partStack.getActivePart() == null || partStack.getActivePart().equals(contributePart)) {
        partStack.setActivePart(contributePart);
      }
    }
  }

  private boolean hasVcsService(Project project) {
    return vcsServiceProvider.getVcsService(project) != null;
  }

  private boolean hasContributionMixin(Project project) {
    return project.getMixins().contains(CONTRIBUTION_PROJECT_TYPE_ID);
  }

  private Promise<Project> addMixin(final Project project) {
    final VcsService vcsService = vcsServiceProvider.getVcsService(project);

    if (vcsService == null || project.getMixins().contains(CONTRIBUTION_PROJECT_TYPE_ID)) {
      return promiseProvider.resolve(project);
    }

    return vcsService
        .getBranchName(project)
        .thenPromise(
            branchName -> {
              MutableProjectConfig mutableConfig = new MutableProjectConfig(project);
              mutableConfig.getMixins().add(CONTRIBUTION_PROJECT_TYPE_ID);
              mutableConfig
                  .getAttributes()
                  .put(CONTRIBUTE_TO_BRANCH_VARIABLE_NAME, singletonList(branchName));

              return project.update().withBody(mutableConfig).send();
            });
  }
}
