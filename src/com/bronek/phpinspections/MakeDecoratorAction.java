package com.bronek.phpinspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class MakeDecoratorAction implements IntentionAction {
    @Nls
    @NotNull
    @Override
    public String getText() {
        return "Make decorator";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Bronek's Family";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        PsiElement element = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());

        if (element instanceof LeafPsiElement) {
            element = element.getParent();
        }

        if (element instanceof ImplementsList) {
            return true;
        }

        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
        SelectionModel selectionModel = editor.getSelectionModel();
        int start = selectionModel.getSelectionStart();

        if (start >= 0) {
            PsiElement element = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());

            if (element instanceof LeafPsiElement) {
                element = element.getParent();
            }

            if (element instanceof ImplementsList) {
                PhpClass phpClass = (PhpClass) element.getParent();

                ImplementsList implementsList = (ImplementsList) element;
                ClassReference classReference = implementsList.getReferenceElements().get(0);
                Collection<PhpClass> phpClasses = PhpIndex.getInstance(project).getInterfacesByFQN(classReference.getFQN());

                for (PhpClass implementsInterface : phpClasses) {
                    String classBody = phpClass.getText().substring(0, phpClass.getTextLength() - 1) + "/** @var " + implementsInterface.getName()
                            + " */\nprivate $inner;";

                    for (Method method : implementsInterface.getMethods()) {
                        classBody += decoratingMethodBody(method);
                    }

                    PhpClass newClass = PhpPsiElementFactory.createFromText(project, PhpClass.class,
                            classBody + "}");

                    if (newClass != null) {
                        phpClass.replace(newClass);
                    }
                }
            }
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @NotNull
    private String decoratingMethodBody(Method method) {
        String classBody = "";

        String[] parametersBody = new String[method.getParameters().length];
        Parameter[] parameters = method.getParameters();

        for (int p = 0; p < parameters.length; p++) {
            parametersBody[p] = "$" + parameters[p].getName();
        }

        classBody += method.getText().substring(0, method.getTextLength() - 1) + "{";

        if (!method.getType().isEmpty() && !method.getType().toString().equals("void")) {
            classBody += "return ";
        }

        classBody += "$this->inner->" + method.getName()+"("
                + String.join(", ", parametersBody) + ");}";

        return classBody;
    }
}
