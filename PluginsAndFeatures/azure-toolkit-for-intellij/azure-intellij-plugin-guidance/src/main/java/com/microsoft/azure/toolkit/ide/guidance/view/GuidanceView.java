package com.microsoft.azure.toolkit.ide.guidance.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.microsoft.azure.toolkit.ide.guidance.Course;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Optional;

public class GuidanceView extends SimpleToolWindowPanel {
    private final Project project;
    private JPanel pnlRoot;
    private CourseView pnlCourse;
    private CoursesView pnlCourses;

    public GuidanceView(final Project project) {
        super(true);
        this.project = project;
        $$$setupUI$$$();
        this.setContent(pnlRoot);
        showCoursesView();
    }

    public void showCoursesView() {
        pnlCourse.setVisible(false);
        pnlCourses.setVisible(true);
        Optional.ofNullable(pnlCourse.getCourse()).ifPresent(Course::dispose);
    }

    public void showCourseView(@Nonnull Course course) {
        AzureTaskManager.getInstance().runOnPooledThread(course::prepare);
        pnlCourses.setVisible(false);
        pnlCourse.setVisible(true);
        pnlCourse.setCourse(course);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.pnlCourse = new CourseView(project);
        this.pnlCourses = new CoursesView(project);
    }
}
