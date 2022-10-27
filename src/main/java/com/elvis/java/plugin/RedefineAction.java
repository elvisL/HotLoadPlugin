package com.elvis.java.plugin;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.apache.commons.io.FilenameUtils;

public class RedefineAction extends AnAction {

    @Override
    @SuppressWarnings("ConstantConditions")
    public void actionPerformed(AnActionEvent e) {

        DataContext dataContext = e.getDataContext();
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        Notifier.setProject(project);

        VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile.getContainingFile();
        PsiClass psiClass = psiJavaFile.getClasses()[0];

        //找到编译的class 文件路径
        Module module = ModuleUtil.findModuleForPsiElement(psiClass);
        VirtualFile compilerOutputVirtualFile = ModuleRootManager.getInstance(module).getModifiableModel().getModuleExtension(CompilerModuleExtension.class).getCompilerOutputPath();
        String compilerOutputVirtualFilePath = compilerOutputVirtualFile.getPath();

        String ideaClassName = getClassName(psiJavaFile);

        compilerOutputVirtualFilePath += "/" + ideaClassName.replace(".", "/") + ".class";
        new AddressInputeJDialog(project, virtualFile, compilerOutputVirtualFilePath, ideaClassName);

    }

    private String getClassName(PsiJavaFile psiJavaFile) {

        String packageName = psiJavaFile.getPackageName();
        String className = FilenameUtils.getBaseName(psiJavaFile.getName());
        String ideaClassName;
        if ("".equals(packageName)) {
            ideaClassName = className;
        } else {
            ideaClassName = packageName + "." + className;
        }
        return ideaClassName;
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        DataContext dataContext = e.getDataContext();
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (psiFile instanceof PsiJavaFile) {
            e.getPresentation().setEnabled(true);
            return;
        }
        e.getPresentation().setEnabled(false);
    }


}
