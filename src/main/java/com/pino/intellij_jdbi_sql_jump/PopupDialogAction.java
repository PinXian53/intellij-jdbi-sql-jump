package com.pino.intellij_jdbi_sql_jump;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class PopupDialogAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent event) {
        // do nothing
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        var editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        var virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            return;
        }

        var fileType = virtualFile.getFileType().getName();
        if (!"JAVA".equals(fileType)) {
            return;
        }

        var psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }

        var psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (psiElement == null) {
            return;
        }

        var project = event.getProject();
        if (project == null) {
            return;
        }

        var parentClassName = psiElement.getParent().getClass().getSimpleName();
        var methodName = psiElement.getText();
        switch (parentClassName) {
            case "PsiMethodImpl":
                doPsiMethodFlow(project, virtualFile, methodName);
                break;
            case "PsiReferenceExpressionImpl":
                // todo 有時間再補
                break;
        }

    }

    private VirtualFile getRootFolder(Project project, VirtualFile file) {
        return ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(file);
    }

    private String getRelativePath(VirtualFile rootFolder, VirtualFile file) {
        if (rootFolder == null) {
            return null;
        }
        var rootFolderPath = rootFolder.getPath();
        var filePath = file.getPath();
        if (!filePath.startsWith(rootFolderPath)) {
            return null;
        }
        return file.getPath().substring(rootFolderPath.length());
    }

    private String getTargetSqlPath(String relativeFilePath, String methodName) {
        var sqlFolderPath = relativeFilePath
            .replace("/java", "")
            .replace(".java", "");
        return "resources%s/%s.sql".formatted(sqlFolderPath, methodName);
    }

    private VirtualFile getTargetFile(VirtualFile rootFolder, String relativeFilePath) {
        return rootFolder.findFileByRelativePath(relativeFilePath);
    }

    private void doPsiMethodFlow(Project project, VirtualFile virtualFile, String methodName) {
        var rootFolder = getRootFolder(project, virtualFile);
        var relativeFilePath = getRelativePath(rootFolder, virtualFile);

        var sqlPath = getTargetSqlPath(relativeFilePath, methodName);
        var targetSqlFile = getTargetFile(rootFolder, sqlPath);

        if (targetSqlFile != null && targetSqlFile.isValid()) {
            FileEditorManager.getInstance(project).openFile(targetSqlFile, true);
        } else {
            var groupId = "Jdbi sql jump: SQL not found";
            var title = "SQL not found!";
            var content = "Not found: %s".formatted(sqlPath);
            var notification = new Notification(groupId, title, content, NotificationType.WARNING);
            Notifications.Bus.notify(notification, project);
        }
    }

}