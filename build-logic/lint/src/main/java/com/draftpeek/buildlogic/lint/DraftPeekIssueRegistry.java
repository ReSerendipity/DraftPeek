package com.draftpeek.buildlogic.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.Vendor;
import com.android.tools.lint.detector.api.Issue;

import java.util.Collections;
import java.util.List;

/**
 * DraftPeek 自定义 Lint 规则注册中心。
 */
public class DraftPeekIssueRegistry extends IssueRegistry {

    @Override
    public List<Issue> getIssues() {
        return Collections.singletonList(BrandComponentDetector.ISSUE);
    }

    @Override
    public Vendor getVendor() {
        return new Vendor(
                "DraftPeek",
                "https://github.com/draftpeek/draftpeek/issues",
                "DraftPeek Team"
        );
    }
}
