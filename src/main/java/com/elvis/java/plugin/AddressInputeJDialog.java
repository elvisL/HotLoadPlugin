package com.elvis.java.plugin;

import com.hello.java.plugin.zk.ServiceNode;
import com.hello.java.plugin.zk.ZkUtils;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.task.ProjectTaskManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.awt.event.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AddressInputeJDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField serverIpField;
    private JLabel jacoco;
    private JRadioButton off;
    private JRadioButton on;
    private JTextField ipPortText;
    private final Project project;
    private final String compilerOutputVirtualFilePath;
    private final String ideaClassName;
    private final VirtualFile virtualFile;

    public AddressInputeJDialog(Project project, VirtualFile virtualFile, String compilerOutputVirtualFilePath, String ideaClassName) {
        String basePath = project.getBasePath();
        String[] split = basePath.split("/");
        List<ServiceNode> serviceNodes = ZkUtils.getServiceNodes(split[split.length - 1]);
        List<ServiceNode> nodes = serviceNodes.stream().filter(node -> node.getStatus() == 1).filter(node -> (node.getTag() == null || Objects.equals(node.getTag(), ""))).collect(Collectors.toList());
        if (nodes.size() == 0) {
            Notifier.notifyError("not find target jvm host from zk,please input by yourself");
        }

        String hostList = nodes.stream().map(ServiceNode::getHost).collect(Collectors.joining(","));
        this.project = project;
        this.ideaClassName = ideaClassName;
        this.compilerOutputVirtualFilePath = compilerOutputVirtualFilePath;
        this.virtualFile = virtualFile;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        serverIpField.setText(hostList);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        open();
    }

    private void onOK() {

        String hosts = serverIpField.getText();
        if(hosts == null || hosts.equals("")){
            Notifier.notify("hot load jvm host cannot be null");
            return;
        }

        // 编译变更的java文件
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Compile Java File") {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {

                progressIndicator.setIndeterminate(false);
                progressIndicator.setFraction(0.10);
                progressIndicator.setText("90% to finish");
                WriteAction.runAndWait(() -> {
                    ProjectTaskManager projectTaskManager = ProjectTaskManager.getInstance(project);
                    Promise<ProjectTaskManager.Result> promise = projectTaskManager.compile(virtualFile);
                    promise.onSuccess(result -> {
                        for (String host : hosts.split(",")) {
                            // 热部署
                            HotLoad.hotLoadByRedefine(host,ipPortText.getText(), ideaClassName, compilerOutputVirtualFilePath, on.isSelected());
                        }

                    });

                });
            }
        });

        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }


    public void open() {
        setTitle("热部署机器IP");
        pack();
        //两个屏幕处理出现问题，跳到主屏幕去了
        setLocationRelativeTo(WindowManager.getInstance().getFrame(this.project));
        setVisible(true);
    }
}
