/*
 * Copyright 2012-2014 Sergey Ignatov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.erlang.inspection;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.intellij.erlang.ErlangFileType;
import org.intellij.erlang.ErlangLanguage;
import org.intellij.erlang.sdk.ErlangSdkRelease;
import org.intellij.erlang.sdk.ErlangSdkType;
import org.intellij.erlang.sdk.ErlangSystemUtil;
import org.intellij.erlang.settings.ErlangExternalToolsConfigurable;
import org.jetbrains.annotations.NotNull;

// todo: extract the common one
public class SetupSDKNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
  private static final Key<EditorNotificationPanel> KEY = Key.create("Setup Erlang SDK");

  private final Project myProject;

  public SetupSDKNotificationProvider(Project project, final EditorNotifications notifications) {
    myProject = project;
    myProject.getMessageBus().connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        notifications.updateAllNotifications();
      }
    });
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return null;
    if (!(file.getFileType() instanceof ErlangFileType)) return null;

    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
    if (psiFile == null || psiFile.getLanguage() != ErlangLanguage.INSTANCE) return null;

    ErlangSdkRelease sdkRelease = ErlangSdkType.getRelease(psiFile);
    if (sdkRelease != null) return null;

    return createPanel(myProject, psiFile);
  }

  @NotNull
  private static EditorNotificationPanel createPanel(@NotNull final Project project, @NotNull final PsiFile file) {
    EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText(ProjectBundle.message("project.sdk.not.defined"));
    panel.createActionLabel(ProjectBundle.message("project.sdk.setup"), () -> {
      if (ErlangSystemUtil.isSmallIde()) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ErlangExternalToolsConfigurable.ERLANG_RELATED_TOOLS);
        return;
      }

      Sdk projectSdk = ProjectSettingsService.getInstance(project).chooseAndSetSdk();
      if (projectSdk == null) return;
      ApplicationManager.getApplication().runWriteAction(() -> {
        Module module = ModuleUtilCore.findModuleForPsiElement(file);
        if (module != null) {
          ModuleRootModificationUtil.setSdkInherited(module);
        }
      });
    });
    return panel;
  }
}