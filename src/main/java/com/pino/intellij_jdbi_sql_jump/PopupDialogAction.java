package com.pino.intellij_jdbi_sql_jump;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PsiNavigateUtil;
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
        if (!"JAVA".equalsIgnoreCase(fileType) && !"SQL".equalsIgnoreCase(fileType)) {
            return;
        }

        var psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }

        var project = event.getProject();
        if (project == null) {
            return;
        }

        switch (fileType.toUpperCase()) {
            case "JAVA":
                jumpToSqlFileFromJavaMethod(project, editor, virtualFile, psiFile);
                break;
            case "SQL":
                jumpToJavaMethodFromSqlFile(project, virtualFile);
                break;
        }
    }

    private void jumpToSqlFileFromJavaMethod(Project project, Editor editor, VirtualFile virtualFile, PsiFile psiFile) {
        var psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (psiElement == null) {
            return;
        }

        var parentClassName = psiElement.getParent().getClass().getSimpleName();
        var methodName = psiElement.getText();
        switch (parentClassName) {
            case "PsiMethodImpl":
                doPsiMethodFlow(project, virtualFile, methodName);
                break;
            case "PsiReferenceExpressionImpl":
                var referenceElement = ((PsiReference) psiElement.getParent()).resolve();
                var referenceVirtualFile = referenceElement.getContainingFile().getVirtualFile();
                doPsiMethodFlow(project, referenceVirtualFile, methodName);
                break;
            default:
                showNotSupportedNotification(project);
                break;
        }
    }

    private void jumpToJavaMethodFromSqlFile(Project project, VirtualFile virtualFile) {
        var rootFolder = getRootFolder(project, virtualFile);
        var targetMethodName = virtualFile.getNameWithoutExtension();
        var relativeFolderPath = getRelativePath(rootFolder, virtualFile.getParent());
        var targetClassName = getTargetClassName(relativeFolderPath);
        var targetPsiMethod = getPsiMethodFromClassNameAndMethodName(project, targetClassName, targetMethodName);
        if (targetPsiMethod != null) {
            PsiNavigateUtil.navigate(targetPsiMethod);
        } else {
            showMethodNotFoundNotification(project, targetClassName, targetMethodName);
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

    /**
     * Get Target Sql Path
     * @param relativeFilePath ex. /java/com/pino/jdbi_demo/dao/ApplicationRepository.java
     * @param methodName ex. getCurrentApplicationId
     * @return ex. resources/com/pino/jdbi_demo/dao/ApplicationRepository/getCurrentApplicationId.sql
     */
    private String getTargetSqlPath(String relativeFilePath, String methodName) {
        var sqlFolderPath = relativeFilePath
            .replace("/java", "")
            .replace(".java", "");
        return "resources%s/%s.sql".formatted(sqlFolderPath, methodName);
    }

    /**
     * Get Target Class Name
     * @param relativeFilePath ex. /resources/com/pino/jdbi_demo/dao/ApplicationRepository
     * @return ex. com.pino.jdbi_demo.dao.ApplicationRepository
     */
    private String getTargetClassName(String relativeFilePath) {
        return relativeFilePath
            .replace("/resources/", "")
            .replace("/", ".");
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
            showSqlNotFoundNotification(project, sqlPath);
        }
    }

    /**
     * Get PsiMethod From Class Name And Method Name
     *
     * @param project    project
     * @param className  className ex. com.example.MyClass
     * @param methodName methodName ex. myMethod
     * @return psiMethod
     */
    private PsiMethod getPsiMethodFromClassNameAndMethodName(Project project, String className, String methodName) {
        var psiFacade = JavaPsiFacade.getInstance(project);
        var psiClass = psiFacade.findClass(className, GlobalSearchScope.allScope(project));
        if (psiClass != null) {
            var psiMethods = psiClass.findMethodsByName(methodName, false);
            if (psiMethods.length > 0) {
                return psiMethods[0]; // return first
            }
        }
        return null; // method not found
    }

    private void showSqlNotFoundNotification(Project project, String sqlPath) {
        var groupId = "Jdbi sql jump: SQL not found";
        var title = "SQL not found!";
        var content = "Not found: %s".formatted(sqlPath);
        var notification = new Notification(groupId, title, content, NotificationType.WARNING);
        Notifications.Bus.notify(notification, project);
    }

    private void showNotSupportedNotification(Project project) {
        var groupId = "Jdbi sql jump: Not supported";
        var title = "Not supported!";
        var content = "This feature is not supported";
        var notification = new Notification(groupId, title, content, NotificationType.WARNING);
        Notifications.Bus.notify(notification, project);
    }

    private void showMethodNotFoundNotification(Project project, String className, String method) {
        var groupId = "Jdbi sql jump: Method not found";
        var title = "Method not found!";
        var content = "Class name: %s , method name: %s".formatted(className, method);
        var notification = new Notification(groupId, title, content, NotificationType.WARNING);
        Notifications.Bus.notify(notification, project);
    }

}