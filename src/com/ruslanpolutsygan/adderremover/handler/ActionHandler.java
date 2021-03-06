package com.ruslanpolutsygan.adderremover.handler;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.actions.PhpNamedElementNode;
import com.jetbrains.php.lang.actions.generation.PhpGenerateFieldAccessorHandlerBase;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.intentions.generators.PhpAccessorMethodData;
import com.jetbrains.php.lang.psi.PhpCodeEditUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.ruslanpolutsygan.adderremover.handler.checkers.MethodChecker;
import com.ruslanpolutsygan.adderremover.handler.generators.MethodGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ActionHandler extends PhpGenerateFieldAccessorHandlerBase {

    private MethodGenerator generator;
    private MethodChecker checker;

    public ActionHandler(MethodGenerator generator, MethodChecker checker) {
        this.generator = generator;
        this.checker = checker;
    }

    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        PhpClass phpClass = PhpCodeEditUtil.findClassAtCaret(editor, file);
        if(phpClass == null) {
            return;
        }

        PhpNamedElementNode[] fields = this.collectFields(phpClass);
        if(fields.length == 0) {
            if(!ApplicationManager.getApplication().isHeadlessEnvironment()) {
                HintManager.getInstance().showErrorHint(editor, this.getErrorMessage());
            }

            return;
        }

        ArrayList<Field> selectedFields = new ArrayList<>();
        PhpNamedElementNode[] chosenMembers = this.chooseMembers(fields, true, file.getProject());

        if(chosenMembers == null) {
            return;
        }

        for (PhpNamedElementNode member : chosenMembers) {
            PsiElement memberPsi = member.getPsiElement();
            if (memberPsi instanceof Field) {
                selectedFields.add((Field) memberPsi);
            }
        }

        for(Field field : selectedFields) {
            for(PhpMethodData methodData : this.generator.generate(field)) {
                PsiElement element = PhpCodeEditUtil.insertClassMemberWithPhpDoc(
                        phpClass, methodData.getMethod(), methodData.getPhpDoc()
                );
                editor.getCaretModel().moveToOffset(element.getTextOffset());
            }
        }
    }

    @Override
    protected PhpAccessorMethodData[] createAccessors(PsiElement psiElement) {
        return new PhpAccessorMethodData[0];
    }

    @Override
    protected boolean isSelectable(PhpClass phpClass, Field field) {
        return !this.checker.hasMethod(field) && this.hasArrayLikeAnnotation(field);
    }

    private boolean hasArrayLikeAnnotation(Field field) {
        PhpDocComment phpDoc = field.getDocComment();
        if(phpDoc == null) {
            return false;
        }

        PhpDocParamTag varTag = phpDoc.getVarTag();
        if(varTag == null) {
            return false;
        }

        Set<String> types = varTag.getType().getTypes();
        for (String type : types) {
            if (type.endsWith("[]")) {
                return true;
            }

            if(type.equals("\\array")) {
                return true;
            }

            if(type.equals("\\Doctrine\\Common\\Collections\\Collection")) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    protected PhpNamedElementNode[] collectFields(@NotNull PhpClass phpClass) {
        TreeMap<String, PhpNamedElementNode> nodes = new TreeMap<>();
        Collection fields = phpClass.getFields();

        for (Object field1 : fields) {
            Field field = (Field) field1;
            if (!field.isConstant() && this.isSelectable(phpClass, field)) {
                nodes.put(field.getName(), new PhpNamedElementNode(field));
            }
        }

        return nodes.values().toArray(new PhpNamedElementNode[nodes.size()]);
    }

    @Override
    protected String getErrorMessage() {
        return "No suitable properties found";
    }

    @Override
    protected boolean containsSetters() {
        return false;
    }

    public boolean startInWriteAction() {
        return true;
    }
}
