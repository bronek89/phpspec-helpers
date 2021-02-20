package com.bronek.phpinspections;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class DescribeClassAction extends CodeInsightAction {

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new CreateSpecHandler();
    }

    private class CreateSpecHandler implements LanguageCodeInsightActionHandler {

        @Override
        public boolean isValidFor(Editor editor, PsiFile psiFile) {
            PsiElement element = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());

            if (element instanceof LeafPsiElement) {
                element = element.getParent();
            }

            if (element instanceof PhpClassImpl) {
                return false;
            }

            PhpClassImpl phpClass = (PhpClassImpl) element;
            System.out.println(phpClass.getNamespaceName());
            return phpClass.getNamespaceName().contains("spec\\");
        }

        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
            PsiElement element = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());

            if (element instanceof LeafPsiElement) {
                element = element.getParent();
            }

            PhpClassImpl phpClass = (PhpClassImpl) element;

            ApplicationManager.getApplication().runWriteAction(
                    () -> {
                        String cleanName = cleanName(psiFile);
                        String namespace = phpClass.getNamespaceName();
                        String content = "<?php \n\nnamespace spec" + namespace.substring(0, namespace.length() - 1) +
                                ";\n\nuse PhpSpec\\ObjectBehavior;" +
                                "\n\nfinal class " + cleanName + "Spec extends ObjectBehavior \n{\n}\n";

                        PsiFileFactory factory = PsiFileFactory.getInstance(project);
                        final PsiFile file = factory.createFileFromText(cleanName + "Spec.php", PhpFileType.INSTANCE, content);
                        CodeStyleManager.getInstance(project).reformat(file);
                        PsiDirectory directory = null;

                        try {
                            VirtualFile projectBaseDir = project.getBaseDir();
                            VirtualFile directoryIfMissing = VfsUtil.createDirectoryIfMissing(projectBaseDir, specFilePath(projectBaseDir, psiFile));
                            directory = PsiManager.getInstance(project).findDirectory(directoryIfMissing);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        PsiElement add = directory.add(file);

                        if (add instanceof PsiFile) {
                            new OpenFileDescriptor(project, ((PsiFile) add).getVirtualFile(), 0).navigate(true);
                        }
                    }
            );
        }

        private String specFilePath(VirtualFile projectBaseDir, PsiFile psiFile) {
            VirtualFile file = psiFile.getVirtualFile();
            String path = file.getPath();
            String newPath = path.replace("src/", "spec/");
            newPath = newPath.replace(psiFile.getName(), "");
            return newPath.replace(projectBaseDir.getPath(), "");
        }

        private String cleanName(PsiFile psiFile) {
            String newName = psiFile.getName();
            return newName.replace(".php", "");
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }
}
