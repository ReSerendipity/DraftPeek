package com.draftpeek.buildlogic.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UCallExpression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 检测直接使用 Material3 组件而非 Brand 封装组件的情况。
 *
 * M3 IconButton           -> BrandIconButton
 * M3 FloatingActionButton -> BrandFAB
 * M3 TopAppBar            -> BrandTopBar
 * M3 Button               -> BrandFilledButton / BrandOutlinedButton
 */
public class BrandComponentDetector extends Detector implements Detector.UastScanner {

    public static final Issue ISSUE = Issue.create(
            "BrandComponentUsage",
            "直接使用 Material3 组件，应使用 Brand 封装组件",
            "为保持设计系统一致性，应使用 Brand 封装组件而非直接调用 M3 组件。\n" +
                    "请将 M3 IconButton/FloatingActionButton/TopAppBar/Button 替换为对应的 Brand 组件。",
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            new Implementation(BrandComponentDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    private static final List<String> TARGET_NAMES = Arrays.asList(
            "IconButton", "FloatingActionButton", "TopAppBar", "Button"
    );

    @Override
    public List<String> getApplicableMethodNames() {
        return TARGET_NAMES;
    }

    @Override
    public void visitMethodCall(JavaContext context, UCallExpression node, PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) return;

        String qualifiedName = containingClass.getQualifiedName();
        if (qualifiedName == null) return;

        // 仅匹配 androidx.compose.material3 包下的组件
        if (!qualifiedName.startsWith("androidx.compose.material3")) return;

        // 排除 Brand 组件自身所在的文件（core/ui / core/designsystem）
        String filePath = context.file.getAbsolutePath().replace('\\', '/');
        if (filePath.contains("/core/ui/") || filePath.contains("/core/designsystem/")) return;

        String name = method.getName();
        String replacement;
        switch (name) {
            case "IconButton":
                replacement = "BrandIconButton";
                break;
            case "FloatingActionButton":
                replacement = "BrandFAB";
                break;
            case "TopAppBar":
                replacement = "BrandTopBar";
                break;
            case "Button":
                replacement = "BrandFilledButton / BrandOutlinedButton";
                break;
            default:
                return;
        }

        context.report(
                ISSUE,
                node,
                context.getLocation(node),
                "直接使用 M3 " + name + "，建议替换为 " + replacement
        );
    }
}
